package com.ruleup.challenge.domain.entity

/** 챌린지 셋업 요구사항. */
data class ChallengeSetupInfo(
    val manual: Boolean,
    // 셋업이 끝나 평가 대상에 들어갔는지(setupStatus == READY).
    val ready: Boolean,
    // 이 챌린지의 인증 방식.
    val verificationMethod: VerificationMethod,
    val requiredPermissions: List<String>,
    // 장소 방식(GPS_PRESENCE·GPS_AVOID)이면 true
    val requiresAnchors: Boolean,
    // 내 앵커가 이미 바인딩됐는지(재진입 시 true 가능).
    val anchorsConfigured: Boolean,
    // 사용 시간 방식(SCREEN_TIME_MAX·MIN)이면 true
    val requiresTargetPackages: Boolean,
)
