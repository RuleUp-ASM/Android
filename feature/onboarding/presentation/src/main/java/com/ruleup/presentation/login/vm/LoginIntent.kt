package com.ruleup.presentation.login.vm

import com.ruleup.core.ui.mvi.MviIntent
import com.ruleup.domain.model.OAuthProvider

sealed interface LoginIntent : MviIntent {
    data object Load : LoginIntent

    data class Login(
        val title: OAuthProvider,
    ) : LoginIntent
}
