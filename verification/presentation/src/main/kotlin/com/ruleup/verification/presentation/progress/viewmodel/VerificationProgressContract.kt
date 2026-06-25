package com.ruleup.verification.presentation.progress.viewmodel

import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.ChallengeProgress

sealed interface VerificationProgressIntent : MviIntent {
    data object Load : VerificationProgressIntent

    data class OpenDetail(
        val challengeId: String,
    ) : VerificationProgressIntent
}

data class VerificationProgressState(
    val isLoading: Boolean = false,
    val challenges: List<ChallengeProgress> = emptyList(),
    val error: String? = null,
) : UiState {
    companion object {
        val initial = VerificationProgressState()
    }
}

sealed interface VerificationProgressReducerEvent : ReducerEvent {
    data object Loading : VerificationProgressReducerEvent

    data class Loaded(
        val challenges: List<ChallengeProgress>,
    ) : VerificationProgressReducerEvent

    data class Failed(
        val message: String,
    ) : VerificationProgressReducerEvent
}

typealias VerificationProgressEffect = NoEffect
