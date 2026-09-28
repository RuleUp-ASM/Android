package com.ruleup.verification.domain.entity

/** 셋업 제출 결과. */
data class ChallengeSetupResult(
    val status: SetupStatus,
    val missing: List<SetupMissing>,
    val serverRadiusM: Float? = null,
) {
    val isReady: Boolean get() = status == SetupStatus.READY
}

/** 셋업 상태. */
enum class SetupStatus {
    PENDING_SETUP,
    READY,
    ;

    companion object {
        fun fromValue(value: String?): SetupStatus = entries.find { it.name == value } ?: PENDING_SETUP
    }
}

/** 미충족 바인딩 항목. */
enum class SetupMissing {
    ANCHORS_REQUIRED,
    TARGET_PACKAGES_REQUIRED,
    ;

    companion object {
        fun fromValue(value: String?): SetupMissing? = entries.find { it.name == value }
    }
}

/** 셋업 앵커 제약. */
object SetupAnchors {
    const val MAX_COUNT: Int = 3

    // 서버 설정 반경의 현재 잠정값.
    const val DEFAULT_RADIUS_M: Float = 500f
}
