package com.ruleup.verification.domain.entity

/**
 * 하루 단위 인증 상태 (명세 sync·progress `todayStatus`). 오늘 인증 결과 조회의 `status` 와 같은 5종이다.
 *
 * [FAIL_EXPECTED] 는 "이대로면 실패"지 확정 실패가 아니다 — 늦은 신호로 뒤집힐 수 있고 이의 신청 창이다.
 */
enum class TodayStatus {
    IN_PROGRESS,
    FAIL_EXPECTED,
    DONE,
    FAILED,
    NOT_TARGET,
    ;

    /** 실패로 확정된 날인가. [FAIL_EXPECTED] 는 아직 뒤집힐 수 있어 실패가 아니다. */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        /** 미인식 값은 null — 진행 중으로 접으면 완료된 날까지 진행 중으로 보인다. */
        fun fromValue(value: String?): TodayStatus? = entries.find { it.name == value }
    }
}

/**
 * 인증 실패/미충족 사유 코드 (명세 3.3 failureReason enum).
 * 미인식 코드는 [UNKNOWN] 으로 떨궈 카피/CTA 매핑이 깨지지 않게 한다(명세 §6.4).
 */
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

/** 진행률 조회 필터 (명세 3.2 query status). */
enum class ProgressFilter(
    val value: String,
) {
    ACTIVE("ACTIVE"),
    ALL("ALL"),
}
