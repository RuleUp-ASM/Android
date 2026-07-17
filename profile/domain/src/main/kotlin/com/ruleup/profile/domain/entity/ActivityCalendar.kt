package com.ruleup.profile.domain.entity

import com.ruleup.entity.user.InterestCategory

/** 일자 종합 상태 (명세 days[].status — 서버 판정, 클라 재계산 없음). */
enum class CalendarDayStatus(
    val value: String,
) {
    // 대상 전부 성공
    ALL_DONE("ALL_DONE"),

    // 일부 성공
    PARTIAL("PARTIAL"),

    // 전부 실패
    FAILED("FAILED"),

    // 당일 판정 대기
    PENDING("PENDING"),

    // 판정 대상 아님
    NOT_TARGET("NOT_TARGET"),
    ;

    companion object {
        fun fromValue(value: String?): CalendarDayStatus = entries.find { it.value == value } ?: NOT_TARGET
    }
}

/** 월 캘린더의 일자별 상태 (판정 대상일만 내려온다 — 없는 날짜는 비대상일). */
data class CalendarDay(
    // YYYY-MM-DD
    val date: String,
    val status: CalendarDayStatus,
    val successCount: Int,
    val targetCount: Int,
)

/** 활동 캘린더 월 응답 (명세: GET /me/calendar?month=YYYY-MM). */
data class ActivityCalendar(
    // YYYY-MM
    val month: String,
    val days: List<CalendarDay>,
)

/** 챌린지별 일자 결과 상태 (명세 items[].status). */
enum class DayItemStatus(
    val value: String,
) {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    PENDING("PENDING"),
    NOT_REQUIRED("NOT_REQUIRED"),
    ;

    companion object {
        fun fromValue(value: String?): DayItemStatus = entries.find { it.value == value } ?: PENDING
    }
}

/** 일자 상세의 챌린지별 결과 (명세: GET /me/calendar/{date} items[]). */
data class CalendarDayItem(
    val challengeId: String,
    val title: String,
    // RoutineOutcome 카테고리 스냅샷 (인식 불가 값은 null)
    val category: InterestCategory?,
    val status: DayItemStatus,
    // AUTO / MANUAL / MANUAL_FALLBACK (확정 전 null)
    val verifiedVia: String?,
    // 확정 시각 ISO-8601 (확정 전 null)
    val verifiedAt: String?,
    // 실패 사유 코드 (예: NO_SIGNAL_RECEIVED)
    val failureReason: String?,
)

/** 일자 상세 (명세: GET /me/calendar/{date}). */
data class CalendarDayDetail(
    val date: String,
    val items: List<CalendarDayItem>,
)
