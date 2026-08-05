package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.entity.user.User

/**
 * 로그인·가입이 돌려주는 세션. 토큰은 core 계약([com.ruleup.domain.token.TokenRepository])이
 * 다루므로 core 에 남고, 이 묶음만 onboarding 소관이다.
 */
data class AuthSession(
    val token: Token,
    val user: User,
)
