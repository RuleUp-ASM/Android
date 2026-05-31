package com.ruleup.domain.model

/**
 * Access / Refresh 토큰 페어.
 */
data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresInSeconds: Int,
)
