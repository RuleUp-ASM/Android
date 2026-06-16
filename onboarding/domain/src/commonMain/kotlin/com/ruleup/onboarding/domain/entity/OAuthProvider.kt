package com.ruleup.onboarding.domain.entity

enum class OAuthProvider(
    val provider: String,
) {
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google"),
    APPLE("apple"),
}
