package com.ruleup.challenge.domain.entity

/**
 * 챌린지 셋업 요구사항(명세: GET /challenges/{id}/setup). 인증을 시작하려면 무엇을 바인딩해야 하는지 안내한다.
 *
 * [requiredPermissions] 는 클라가 OS 권한을 스스로 재확인하는 참고용 목록(서버 미보관).
 * [requiresAnchors]/[requiresTargetPackages] 로 지도(앵커)·대상 앱 등록이 실제로 필요한지 판단해
 * 필요한 등록만 노출한다. [manual] 이면 자동 셋업 없이 수동 인증이다.
 */
data class ChallengeSetupInfo(
    val manual: Boolean,
    val requiredPermissions: List<String>,
    // GPS_PRESENCE 방식이면 true — 인증 장소(지도 앵커) 바인딩 필요.
    val requiresAnchors: Boolean,
    // 내 앵커가 이미 바인딩됐는지(재진입 시 true 가능).
    val anchorsConfigured: Boolean,
    // SCREEN_TIME 방식이면 true — 대상 앱 선택 필요.
    val requiresTargetPackages: Boolean,
)
