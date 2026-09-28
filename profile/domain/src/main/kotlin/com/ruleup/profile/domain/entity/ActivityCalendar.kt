package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.category.Category

/** 일자 종합 상태. */
enum class CalendarDayStatus(
    val value: String,
) {
    ALL_DONE("ALL_DONE"),
    PARTIAL("PARTIAL"),
    FAILED("FAILED"),

    // 실패 예정
    FAIL_EXPECTED("FAIL_EXPECTED"),

    // 오늘, 아직 판정 전
    IN_PROGRESS("IN_PROGRESS"),
    ;

    companion object {
        /** 미인식 값은 null */
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

/** 활동 캘린더 월 응답. */
data class ActivityCalendar(
    // YYYY-MM
    val month: String,
    val days: List<CalendarDay>,
)

/** 챌린지별 일자 결과 상태. */
enum class DayItemStatus(
    val value: String,
) {
    IN_PROGRESS("IN_PROGRESS"),

    // 실패 예정
    FAIL_EXPECTED("FAIL_EXPECTED"),

    DONE("DONE"),
    FAILED("FAILED"),
    ;

    companion object {
        /** 미인식 값은 null */
        fun fromValue(value: String?): DayItemStatus? = entries.find { it.value == value }
    }
}

/** 일자 상세의 이의 가능 여부. */
data class DayItemAppeal(
    val eligible: Boolean,
    // 신청 마감 경계(ISO-8601).
    val eligibleUntil: String?,
)

/** 일자 상세의 챌린지별 결과. */
data class CalendarDayItem(
    val challengeId: String,
    val title: String,
    // RoutineOutcome 카테고리 스냅샷 (인식 불가 값은 null)
    val category: Category?,
    val status: DayItemStatus?,
    // 이의 신청 대상 인증 건 ID.
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

/** 일자 상세. */
data class CalendarDayDetail(
    val date: String,
    val items: List<CalendarDayItem>,
)
