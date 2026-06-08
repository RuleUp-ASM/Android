package com.ruleup.onboarding.domain.entity

data class OAuthAuthorization(
    val provider: OAuthProvider,
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)
