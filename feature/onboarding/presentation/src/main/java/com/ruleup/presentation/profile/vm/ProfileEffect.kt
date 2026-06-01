package com.ruleup.presentation.profile.vm

import com.ruleup.core.ui.mvi.MviEffect

sealed interface ProfileEffect : MviEffect {
    data class ShowError(
        val message: String,
    ) : ProfileEffect
}
