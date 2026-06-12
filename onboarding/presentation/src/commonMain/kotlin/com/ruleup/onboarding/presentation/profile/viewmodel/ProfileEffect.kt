package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.ui.mvi.MviEffect

sealed interface ProfileEffect : MviEffect {
    data class ShowError(
        val message: String,
    ) : ProfileEffect
}
