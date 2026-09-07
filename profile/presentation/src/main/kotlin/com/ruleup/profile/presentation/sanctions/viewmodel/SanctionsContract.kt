package com.ruleup.profile.presentation.sanctions.viewmodel

import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface SanctionsIntent : MviIntent {
    data object Load : SanctionsIntent

    data object Back : SanctionsIntent
}

data class SanctionsState(
    val isLoading: Boolean,
    val history: SanctionHistory?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            SanctionsState(
                isLoading = true,
                history = null,
                errorMessage = null,
            )
    }
}

sealed interface SanctionsReducerEvent : ReducerEvent {
    data object Loading : SanctionsReducerEvent

    data class Loaded(
        val history: SanctionHistory,
    ) : SanctionsReducerEvent

    data class Failed(
        val message: String,
    ) : SanctionsReducerEvent
}

/** 열람 전용 화면 — 이의 제기 버튼이 없어 단발성 이펙트도 없다. */
typealias SanctionsEffect = NoEffect
