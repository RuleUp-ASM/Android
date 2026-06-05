package com.ruleup.entity.onboarding

data class OAuthAuthorization(
    val provider: OAuthProvider,
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)
