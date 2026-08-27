package com.ruleup.onboarding.domain.auth.entity

/**
 * 지원하는 소셜 로그인 제공자. 네이버·애플은 MVP 범위 밖이라 두지 않는다 — enum 에만 남겨 두면
 * 눌러도 "미지원 provider" 로 끝나는 버튼과 도달 불가 분기가 계속 따라다닌다.
 */
enum class OAuthProvider(
    val provider: String,
) {
    KAKAO("kakao"),
    GOOGLE("google"),
}
