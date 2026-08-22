package com.ruleup.verification.domain.entity

/**
 * 하루 단위 인증 상태 (명세 3.2/3.3 todayStatus, BE 소유).
 *
 * ⚠️ [PENDING] 은 "진행 중"이지 실패가 아니다. 특히 MAX·부재·시간창 루틴은 창이 닫혀야
 * [SUCCESS] 로 확정되므로 그 전엔 계속 [PENDING] 이다(명세 §6.4). 위반 시에만 [FAILED].
 */
enum class TodayStatus {
    SUCCESS,
    PENDING,
    FAILED,
    NOT_TARGET,
    ;

    /**
     * 실패로 확정된 날인가 — 실패 카피·이의 제기 노출의 기준.
     *
     * [PENDING] 은 아직 판정 전이고 [NOT_TARGET] 은 애초에 대상이 아니다. 둘 다 실패가 아니다.
     */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        // 서버가 새 값을 추가해도 깨지지 않도록 미인식은 보수적으로 PENDING(진행 중)으로 떨군다.
        fun fromValue(value: String?): TodayStatus = entries.find { it.name == value } ?: PENDING
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
