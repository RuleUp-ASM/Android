package com.ruleup.domain.auth.model

data class OAuthAuthorization(
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)
