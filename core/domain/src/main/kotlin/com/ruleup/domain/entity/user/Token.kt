package com.ruleup.domain.entity.user

data class Token(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresInSeconds: Int,
)
