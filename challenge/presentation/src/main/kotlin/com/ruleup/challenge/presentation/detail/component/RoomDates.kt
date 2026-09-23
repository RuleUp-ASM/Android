package com.ruleup.challenge.presentation.detail.component

import com.ruleup.domain.time.ServiceDate
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 방 피드·랭킹의 날짜 표시.
 *
 * 판정 경계가 KST 하루 단위라 **기기 타임존이 아니라 KST 로 옮긴다** — 기기 기준으로 바꾸면
 * 해외 로밍 중에 어제 성공이 그저께로 밀린다.
 *
 * **문자열을 자르지 않는다.** 서버 응답에 `Z`(UTC) 표기가 섞여 있어, 오프셋을 무시하고 자르면
 * 10:17 이 01:17 로 보이고 자정 근처에서는 날짜까지 하루 어긋난다(ROOM-01).
 * 오프셋이 아예 없는 값은 이미 서비스 기준 시각으로 보고 그대로 쓴다.
 */
private fun isoDatePart(iso: String): String = ServiceDate.atZone(iso)?.toLocalDate()?.toString() ?: iso.substringBefore('T')

private fun parseDateOrNull(isoDate: String): LocalDate? =
    try {
        LocalDate.parse(isoDate)
    } catch (_: DateTimeParseException) {
        null
    }

/** 피드 날짜 구분 헤더 — "오늘" / "어제" / "7월 25일". 파싱 불가면 날짜 문자열 그대로. */
internal fun feedDateHeader(
    iso: String,
    today: LocalDate = ServiceDate.today(),
): String {
    val datePart = isoDatePart(iso)
    val date = parseDateOrNull(datePart) ?: return datePart
    return when (date) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> "${date.monthValue}월 ${date.dayOfMonth}일"
    }
}

/** 같은 날 묶음 판정용 키. 표시 문구가 아니라 그룹핑 전용이다. */
internal fun feedDateKey(iso: String): String = isoDatePart(iso)

/** 피드 아이템 시각 — "06:12"(KST). 시각 부분이 없으면 빈 문자열. */
internal fun feedTimeLabel(iso: String): String {
    ServiceDate.atZone(iso)?.let { return "%02d:%02d".format(it.hour, it.minute) }
    val time = iso.substringAfter('T', "")
    if (time.length < 5) return ""
    return time.take(5)
}

/**
 * 실패 아이템 문구. 실패는 이의 기간(1일)이 지난 뒤에 흐르므로 **발생일보다 늦게** 보인다.
 * 그래서 날짜를 명시한 과거형으로 적는다 — 지금 실패한 것처럼 읽히면 사실이 왜곡된다.
 */
internal fun failDateLabel(failDate: String?): String {
    val date = failDate?.let(::parseDateOrNull) ?: return "인증하지 못한 날이 있어요"
    return "${date.monthValue}월 ${date.dayOfMonth}일 인증을 놓쳤어요"
}

/**
 * 방 밖 랭킹 갱신 시각. 하루 1회 03시 배치라 실시간이 아니며, 이 문구가 "방금 인증했는데 왜
 * 안 오르지"에 대한 답이다.
 */
internal fun rankingUpdatedLabel(
    updatedAt: String?,
    today: LocalDate = ServiceDate.today(),
): String {
    val datePart = updatedAt?.let(::isoDatePart) ?: return "매일 1회 갱신"
    val date = parseDateOrNull(datePart) ?: return "매일 1회 갱신"
    val hour = feedTimeLabel(updatedAt).substringBefore(':').ifBlank { "03" }
    val day = if (date == today) "오늘" else "${date.monthValue}.${date.dayOfMonth}"
    return "$day ${hour}시 기준 · 매일 1회 갱신"
}

/**
 * 진행 기간 표기 — "7.1 – 8.11 · 6주" (Figma 1134:221).
 * 주 수는 올림한다 — 6주 하고 이틀 남은 방을 5주로 적으면 실제보다 짧게 읽힌다.
 */
internal fun periodLabel(
    start: String,
    end: String,
): String {
    val from = parseDateOrNull(isoDatePart(start))
    val to = parseDateOrNull(isoDatePart(end))
    if (from == null || to == null) return "$start ~ $end"
    val range = "${from.monthValue}.${from.dayOfMonth} – ${to.monthValue}.${to.dayOfMonth}"
    val days =
        java.time.temporal.ChronoUnit.DAYS
            .between(from, to)
            .toInt() + 1
    if (days <= 0) return range
    val weeks = (days + 6) / 7
    return "$range · ${weeks}주"
}

/**
 * 이의 신청 마감일 문구 — "오늘" / "내일" / "7월 26일".
 *
 * 서버가 주는 `eligibleUntil` 은 **경계 시각**이라 그 날짜를 그대로 쓰면 하루 늦게 안내한다.
 * 기한 경계는 실패 확정과 같은 시각(귀속일 이틀 뒤 00:00 KST)이라, 낼 수 있는 마지막 날은 그 하루 전이다.
 */
internal fun appealDeadlineLabel(
    eligibleUntil: String,
    today: LocalDate = ServiceDate.today(),
): String? {
    val boundary = parseDateOrNull(isoDatePart(eligibleUntil)) ?: return null
    return when (val lastDay = boundary.minusDays(1)) {
        today -> "오늘"
        today.plusDays(1) -> "내일"
        else -> "${lastDay.monthValue}월 ${lastDay.dayOfMonth}일"
    }
}
