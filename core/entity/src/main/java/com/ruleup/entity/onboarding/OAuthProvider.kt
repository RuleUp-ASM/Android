package com.ruleup.entity.onboarding

enum class OAuthProvider(
    val provider: String,
) {
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google"),
    APPLE("apple"),
}
