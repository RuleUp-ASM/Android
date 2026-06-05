package com.ruleup.presentation.intro.viewmodel

import com.ruleup.entity.onboarding.OAuthAuthorization
import com.ruleup.entity.onboarding.OAuthProvider
import com.ruleup.ui.mvi.MviIntent

sealed interface LoginIntent : MviIntent {
    data object Load : LoginIntent

    data class LoginClicked(
        val provider: OAuthProvider,
    ) : LoginIntent

    data class AuthorizationReceived(
        val authorization: OAuthAuthorization,
    ) : LoginIntent

    data class AuthFailed(
        val error: Throwable,
    ) : LoginIntent
}
