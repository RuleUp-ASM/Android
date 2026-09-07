package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.category.Category

/**
 * 일자 종합 상태 (명세 days[].status — 서버 판정, 클라 재계산 없음).
 * "대상 아님" 값이 없는 건 판정 대상일만 내려오기 때문이다 — 응답에 없는 날짜가 곧 비대상일이다.
 */
enum class CalendarDayStatus(
    val value: String,
) {
    ALL_DONE("ALL_DONE"),
    PARTIAL("PARTIAL"),
    FAILED("FAILED"),

    // 최종 재평가 중 — 실패가 아니다
    CHECKING("CHECKING"),

    // 오늘, 아직 판정 전
    IN_PROGRESS("IN_PROGRESS"),
    ;

    companion object {
        /** 미인식 값은 null — 캘린더 셀을 특정 색으로 칠하면 그 날짜에 대해 거짓말을 하게 된다. */
        fun fromValue(value: String?): CalendarDayStatus? = entries.find { it.value == value }
    }
}

/** 월 캘린더의 일자별 상태 (판정 대상일만 내려온다 — 없는 날짜는 비대상일). */
data class CalendarDay(
    // YYYY-MM-DD
    val date: String,
    val status: CalendarDayStatus?,
    val successCount: Int,
    val targetCount: Int,
)

/** 활동 캘린더 월 응답 (명세: GET /me/calendar?month=YYYY-MM). */
data class ActivityCalendar(
    // YYYY-MM
    val month: String,
    val days: List<CalendarDay>,
)

/**
 * 챌린지별 일자 결과 상태 (명세 items[].status). 오늘 인증 카드와 같은 어휘를 쓴다 —
 * 이의가 인용되면 [FAILED] 가 [DONE] 으로 소급된다.
 */
enum class DayItemStatus(
    val value: String,
) {
    IN_PROGRESS("IN_PROGRESS"),

    // 최종 재평가 중 — 실패가 아니다
    CHECKING("CHECKING"),

    DONE("DONE"),
    FAILED("FAILED"),
    ;

    companion object {
        /** 미인식 값은 null — 모르는 상태를 완료나 실패 어느 쪽으로도 접지 않는다. */
        fun fromValue(value: String?): DayItemStatus? = entries.find { it.value == value }
    }
}

/**
 * 일자 상세의 이의 가능 여부 (명세 items[].appeal — FAILED 일 때만).
 * `remainingThisMonth`·`LIMIT_EXCEEDED` 는 읽지 않는다 — 이의 횟수 한도가 폐기됐다(챌린지 정책 §7.2).
 */
data class DayItemAppeal(
    val eligible: Boolean,
    // 신청 마감 경계(ISO-8601). 실패 확정과 같은 시각이라 귀속일 이틀 뒤 00:00 KST 가 내려온다
    val eligibleUntil: String?,
)

/** 일자 상세의 챌린지별 결과 (명세: GET /me/calendar/{date} items[]). */
data class CalendarDayItem(
    val challengeId: String,
    val title: String,
    // RoutineOutcome 카테고리 스냅샷 (인식 불가 값은 null)
    val category: Category?,
    val status: DayItemStatus?,
    // 이의 신청 대상 인증 건 ID. 없으면 실패 항목이라도 이의로 들어갈 경로가 없다
    val verificationId: String?,
    // AUTO / MANUAL (확정 전 null)
    val verifiedVia: String?,
    // 확정 시각 ISO-8601 (확정 전 null)
    val confirmedAt: String?,
    // 실패 사유 코드 (예: NO_SIGNAL_RECEIVED)
    val failureReason: String?,
    // FAILED 일 때만 내려온다
    val appeal: DayItemAppeal?,
)

/** 일자 상세 (명세: GET /me/calendar/{date}). */
data class CalendarDayDetail(
    val date: String,
    val items: List<CalendarDayItem>,
)
