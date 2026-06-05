package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.ui.mvi.UiState

data class LoginState(
    val isLoading: Boolean = false,
) : UiState {
    companion object {
        val initial = LoginState()
    }
}
