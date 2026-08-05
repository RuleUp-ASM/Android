package com.ruleup.onboarding.domain.auth.entity

data class OAuthAuthorization(
    val provider: OAuthProvider,
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)
