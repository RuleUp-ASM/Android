package com.ruleup.entity.user

data class AuthSession(
    val token: Token,
    val user: User,
)

data class Token(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresInSeconds: Int,
)
