package com.ruleup.presentation.auth.login.vm

import com.ruleup.core.ui.mvi.UiState
import com.ruleup.domain.auth.model.OAuthProvider

data class LoginState(
    val provider: OAuthProvider,
    val isLoading: Boolean = true,
) : UiState {
    companion object {
        val initial = LoginState(OAuthProvider.NONE)
    }
}
