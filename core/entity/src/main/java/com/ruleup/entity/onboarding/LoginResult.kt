package com.ruleup.entity.onboarding

sealed interface LoginResult {
    data object GoMain : LoginResult

    data class GoSignup(
        val signupToken: String,
    ) : LoginResult
}
