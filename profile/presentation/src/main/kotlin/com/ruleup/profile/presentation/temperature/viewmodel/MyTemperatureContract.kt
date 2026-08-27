package com.ruleup.profile.presentation.temperature.viewmodel

import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyTemperatureIntent : MviIntent {
    data object Load : MyTemperatureIntent

    data object OpenHistory : MyTemperatureIntent

    data object Back : MyTemperatureIntent
}

data class MyTemperatureState(
    val isLoading: Boolean,
    val detail: ReputationDetail?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyTemperatureState(
                isLoading = true,
                detail = null,
                errorMessage = null,
            )
    }
}

sealed interface MyTemperatureReducerEvent : ReducerEvent {
    data object Loading : MyTemperatureReducerEvent

    data class Loaded(
        val detail: ReputationDetail,
    ) : MyTemperatureReducerEvent

    data class Failed(
        val message: String,
    ) : MyTemperatureReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyTemperatureEffect = NoEffect
