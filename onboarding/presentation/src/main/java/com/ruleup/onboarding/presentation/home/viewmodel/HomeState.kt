package com.ruleup.onboarding.presentation.home.viewmodel

import com.ruleup.ui.mvi.UiState

data class HomeState(
    val isLoggingOut: Boolean,
) : UiState {
    companion object {
        val initial = HomeState(isLoggingOut = false)
    }
}
