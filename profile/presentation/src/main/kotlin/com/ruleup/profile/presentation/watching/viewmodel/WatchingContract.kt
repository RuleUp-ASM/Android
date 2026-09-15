package com.ruleup.profile.presentation.watching.viewmodel

import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface WatchingIntent : MviIntent {
    data object Load : WatchingIntent

    data object Back : WatchingIntent
}

sealed interface WatchingEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : WatchingEffect
}

data class WatchingState(
    val isLoading: Boolean,
    val items: List<Watching>,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            WatchingState(
                isLoading = true,
                items = emptyList(),
                errorMessage = null,
            )
    }
}

sealed interface WatchingReducerEvent : ReducerEvent {
    data object Loading : WatchingReducerEvent

    data class Loaded(
        val items: List<Watching>,
    ) : WatchingReducerEvent

    data class Failed(
        val message: String,
    ) : WatchingReducerEvent
}
