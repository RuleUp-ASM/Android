package com.ruleup.domain.model

/**
 * 로그인/가입이 완료되어 정식 토큰과 사용자 정보가 확정된 상태.
 */
data class AuthSession(
    val tokens: Tokens,
    val user: User,
)
