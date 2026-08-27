package com.ruleup.profile.presentation.history.viewmodel

import com.ruleup.profile.domain.entity.ReputationHistory
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ReputationHistoryIntent : MviIntent {
    data object Load : ReputationHistoryIntent

    data object Back : ReputationHistoryIntent
}

data class ReputationHistoryState(
    val isLoading: Boolean,
    val history: ReputationHistory?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            ReputationHistoryState(
                isLoading = true,
                history = null,
                errorMessage = null,
            )
    }
}

sealed interface ReputationHistoryReducerEvent : ReducerEvent {
    data object Loading : ReputationHistoryReducerEvent

    data class Loaded(
        val history: ReputationHistory,
    ) : ReputationHistoryReducerEvent

    data class Failed(
        val message: String,
    ) : ReputationHistoryReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias ReputationHistoryEffect = NoEffect
