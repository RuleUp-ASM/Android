package com.ruleup.challenge.domain.entity

/**
 * 챌린지 셋업 요구사항(명세: GET /challenges/{id}/setup). 인증을 시작하려면 무엇을 바인딩해야 하는지 안내한다.
 *
 * [requiredPermissions] 는 클라가 OS 권한을 스스로 재확인하는 참고용 목록(서버 미보관).
 * [requiresAnchors]/[requiresTargetPackages] 로 지도(앵커)·대상 앱 등록이 실제로 필요한지 판단해
 * 필요한 등록만 노출한다. [manual] 이면 자동 셋업 없이 수동 인증이다.
 *
 * [ready] 가 false 면 서버가 **신호를 받아두되 판정을 건너뛴다.** 화면이 "왜 아무 판정도 안 나오는지"를
 * 설명할 수 있는 유일한 근거라 응답에서 버리지 않는다.
 */
data class ChallengeSetupInfo(
    val manual: Boolean,
    // 셋업이 끝나 평가 대상에 들어갔는지(setupStatus == READY).
    val ready: Boolean,
    // 이 챌린지의 인증 방식. requiresAnchors·requiresTargetPackages 가 여기서 파생된다.
    val verificationMethod: VerificationMethod,
    val requiredPermissions: List<String>,
    // GPS_PRESENCE 방식이면 true — 인증 장소(지도 앵커) 바인딩 필요.
    val requiresAnchors: Boolean,
    // 내 앵커가 이미 바인딩됐는지(재진입 시 true 가능).
    val anchorsConfigured: Boolean,
    // SCREEN_TIME 방식이면 true — 대상 앱 선택 필요.
    val requiresTargetPackages: Boolean,
)
