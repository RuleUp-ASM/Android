package com.ruleup.domain.model

/**
 * 지원하는 OAuth 제공자. API 경로의 {provider} 값으로 사용된다.
 */
enum class OAuthProvider(
    val value: String,
) {
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google"),
    APPLE("apple"),
    NONE("none")
}
