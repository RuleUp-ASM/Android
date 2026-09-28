package com.ruleup.onboarding.domain.auth.entity

/** 지원하는 소셜 로그인 제공자. */
enum class OAuthProvider(
    val provider: String,
) {
    KAKAO("kakao"),
    GOOGLE("google"),
}
