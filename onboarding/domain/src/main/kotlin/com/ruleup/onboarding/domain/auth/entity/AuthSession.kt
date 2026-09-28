package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.entity.user.User

/** 로그인·가입이 돌려주는 세션. */
data class AuthSession(
    val token: Token,
    val user: User,
)
