package com.ruleup.challenge.presentation.detail.component

import com.ruleup.domain.time.ServiceDate
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** 방 피드·랭킹의 날짜 표시. */
private fun isoDatePart(iso: String): String = ServiceDate.atZone(iso)?.toLocalDate()?.toString() ?: iso.substringBefore('T')

private fun parseDateOrNull(isoDate: String): LocalDate? =
    try {
        LocalDate.parse(isoDate)
    } catch (_: DateTimeParseException) {
        null
    }

/** 피드 날짜 구분 헤더 */
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

/** 같은 날 묶음 판정용 키. */
internal fun feedDateKey(iso: String): String = isoDatePart(iso)

/** 피드 아이템 시각 */
internal fun feedTimeLabel(iso: String): String {
    ServiceDate.atZone(iso)?.let { return "%02d:%02d".format(it.hour, it.minute) }
    val time = iso.substringAfter('T', "")
    if (time.length < 5) return ""
    return time.take(5)
}

/** 실패 아이템 문구. */
internal fun failDateLabel(failDate: String?): String {
    val date = failDate?.let(::parseDateOrNull) ?: return "인증하지 못한 날이 있어요"
    return "${date.monthValue}월 ${date.dayOfMonth}일 인증을 놓쳤어요"
}

/** 방 밖 랭킹 갱신 시각. */
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

/** 진행 기간 표기 */
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

/** 이의 신청 마감일 문구 */
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
