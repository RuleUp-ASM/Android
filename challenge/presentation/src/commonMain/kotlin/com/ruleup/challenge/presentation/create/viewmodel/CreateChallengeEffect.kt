package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.ui.mvi.MviEffect

sealed interface CreateChallengeEffect : MviEffect {
    data class ShowError(
        val message: String,
    ) : CreateChallengeEffect
}
