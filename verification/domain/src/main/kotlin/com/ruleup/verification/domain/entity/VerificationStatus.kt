package com.ruleup.verification.domain.entity

/** 하루 단위 인증 상태. */
enum class TodayStatus {
    IN_PROGRESS,
    FAIL_EXPECTED,
    DONE,
    FAILED,
    NOT_TARGET,
    ;

    /** 실패로 확정된 날인가. */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        /** 미인식 값은 null */
        fun fromValue(value: String?): TodayStatus? = entries.find { it.name == value }
    }
}

/** 인증 실패/미충족 사유 코드. */
enum class FailureReason {
    OUT_OF_GEOFENCE,
    INSUFFICIENT_DWELL,
    ENTERED_AVOID_ZONE,
    INSUFFICIENT_DISTANCE,
    INSUFFICIENT_STEPS,
    UNTRUSTED_HEALTH_SOURCE,
    INSUFFICIENT_USAGE,
    USAGE_EXCEEDED,
    WOKE_UP_LATE,
    PHONE_USED_IN_BLOCK_WINDOW,
    SLEPT_LATE,
    INSUFFICIENT_SLEEP,
    PERIOD_QUOTA_MISSED,
    NO_SIGNAL_RECEIVED,
    PERMISSION_MISSING,
    GEOFENCE_NOT_CONFIGURED,
    METHOD_NOT_SUPPORTED_ON_PLATFORM,
    MANUAL_NOT_SUBMITTED,
    FALLBACK_LIMIT_EXCEEDED,
    UNKNOWN,
    ;

    companion object {
        fun fromValue(value: String?): FailureReason? =
            when (value) {
                null -> null
                else -> entries.find { it.name == value } ?: UNKNOWN
            }
    }
}

/** 진행률 조회 필터. */
enum class ProgressFilter(
    val value: String,
) {
    ACTIVE("ACTIVE"),
    ALL("ALL"),
}
