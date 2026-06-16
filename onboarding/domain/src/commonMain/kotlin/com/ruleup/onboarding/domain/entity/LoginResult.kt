package com.ruleup.onboarding.domain.entity

sealed interface LoginResult {
    data object GoMain : LoginResult

    data class GoSignup(
        val signupToken: String,
    ) : LoginResult
}
