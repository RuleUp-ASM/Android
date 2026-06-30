package com.ruleup.verification.domain.entity

/**
 * 셋업 제출 결과(명세 setup). 앵커·대상앱 바인딩이 모두 충족되면 [SetupStatus.READY],
 * 미충족이면 [missing] 에 부족 항목을 담아 [SetupStatus.PENDING_SETUP] 로 온다.
 */
data class ChallengeSetupResult(
    val status: SetupStatus,
    val missing: List<SetupMissing>,
) {
    val isReady: Boolean get() = status == SetupStatus.READY
}

/** 셋업 상태(명세 setup). 평가 대상 진입 여부. */
enum class SetupStatus {
    PENDING_SETUP,
    READY,
    ;

    companion object {
        fun fromValue(value: String?): SetupStatus = entries.find { it.name == value } ?: PENDING_SETUP
    }
}

/**
 * 미충족 바인딩 항목(명세 setup missing[]). 서버가 새 항목을 추가해도 무시되도록 알 수 없는 값은 버린다.
 */
enum class SetupMissing {
    ANCHORS_REQUIRED,
    TARGET_PACKAGES_REQUIRED,
    ;

    companion object {
        fun fromValue(value: String?): SetupMissing? = entries.find { it.name == value }
    }
}

/** 셋업 앵커 제약(명세 setup). 반경 500~5000m, 최대 10개. */
object SetupAnchors {
    const val MIN_RADIUS_M: Float = 500f
    const val MAX_RADIUS_M: Float = 5000f
    const val MAX_COUNT: Int = 10
}
