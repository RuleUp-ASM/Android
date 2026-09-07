package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyTierHistoryIntent : MviIntent {
    data object Load : MyTierHistoryIntent

    data object Back : MyTierHistoryIntent
}

data class MyTierHistoryState(
    val isLoading: Boolean,
    val history: TierHistory?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyTierHistoryState(
                isLoading = true,
                history = null,
                errorMessage = null,
            )
    }
}

sealed interface MyTierHistoryReducerEvent : ReducerEvent {
    data object Loading : MyTierHistoryReducerEvent

    data class Loaded(
        val history: TierHistory,
    ) : MyTierHistoryReducerEvent

    data class Failed(
        val message: String,
    ) : MyTierHistoryReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyTierHistoryEffect = NoEffect
