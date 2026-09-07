package com.ruleup.verification.domain.entity

/**
 * 셋업 제출 결과(명세 setup). 앵커·대상앱 바인딩이 모두 충족되면 [SetupStatus.READY],
 * 미충족이면 [missing] 에 부족 항목을 담아 [SetupStatus.PENDING_SETUP] 로 온다.
 *
 * [serverRadiusM] 은 서버가 정한 인증 반경(m)이다 — OS 지오펜스 등록 반경과 지도 원이 이 값을
 * 따라야 서버 판정 반경과 어긋나지 않는다. 응답에 없으면 null 이고 호출부가 폴백을 쓴다.
 */
data class ChallengeSetupResult(
    val status: SetupStatus,
    val missing: List<SetupMissing>,
    val serverRadiusM: Float? = null,
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

/**
 * 셋업 앵커 제약(인증 정책 §1.1 · 명세 setup).
 *
 * 반경은 **사용자가 고르는 값이 아니라 서버 설정 단일값**이라 요청에 싣지 않는다. 서버가
 * `serverRadiusM` 으로 내려주며, [DEFAULT_RADIUS_M] 은 그 값을 아직 못 받았을 때만 쓰는 폴백이다.
 */
object SetupAnchors {
    const val MAX_COUNT: Int = 3

    // 서버 설정 반경의 현재 잠정값. 성능 테스트 후 서버에서 조정된다.
    const val DEFAULT_RADIUS_M: Float = 500f
}
