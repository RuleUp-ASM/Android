package com.ruleup.presentation.auth.login.vm

import com.ruleup.core.ui.mvi.MviIntent
import com.ruleup.domain.auth.model.OAuthProvider

sealed interface LoginIntent : MviIntent {
    data object Load : LoginIntent

    data class Login(
        val title: OAuthProvider,
    ) : LoginIntent
}
