package com.ruleup.challenge.domain.entity

/** 챌린지 단위 월 캘린더의 하루 상태. */
enum class ChallengeDayStatus(
    val value: String,
) {
    DONE("DONE"),

    FAILED("FAILED"),

    // 귀속일은 지났고 확정 전
    FAIL_EXPECTED("FAIL_EXPECTED"),

    // 오늘.
    IN_PROGRESS("IN_PROGRESS"),
    ;

    /** 확정된 실패가 아니다 */
    val isSettledFailure: Boolean
        get() = this == FAILED

    companion object {
        /** 미지 값은 null */
        fun fromValue(value: String?): ChallengeDayStatus? = entries.find { it.value == value }
    }
}

/** 판정 대상일 하루. */
data class ChallengeCalendarDay(
    // YYYY-MM-DD
    val date: String,
    val status: ChallengeDayStatus?,
    // 그날 인증 건.
    val verificationId: String?,
    // 이의 신청 가능 여부
    val appealable: Boolean,
)

/** 챌린지 월 캘린더. */
data class ChallengeCalendar(
    val challengeId: String,
    // YYYY-MM
    val month: String,
    val days: List<ChallengeCalendarDay>,
) {
    /** 날짜로 찾는다. */
    fun dayOf(date: String): ChallengeCalendarDay? = days.find { it.date == date }
}
