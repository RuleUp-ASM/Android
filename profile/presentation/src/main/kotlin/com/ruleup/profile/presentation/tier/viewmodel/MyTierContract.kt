package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyTierIntent : MviIntent {
    data object Load : MyTierIntent

    data object OpenHistory : MyTierIntent

    data object Back : MyTierIntent
}

data class MyTierState(
    val isLoading: Boolean,
    val tier: MyTier?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyTierState(
                isLoading = true,
                tier = null,
                errorMessage = null,
            )
    }
}

sealed interface MyTierReducerEvent : ReducerEvent {
    data object Loading : MyTierReducerEvent

    data class Loaded(
        val tier: MyTier,
    ) : MyTierReducerEvent

    data class Failed(
        val message: String,
    ) : MyTierReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyTierEffect = NoEffect
