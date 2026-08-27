package com.ruleup.profile.presentation.appeals.viewmodel

import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.AppealHistoryItem

sealed interface MyAppealsIntent : MviIntent {
    data object Load : MyAppealsIntent

    data object Retry : MyAppealsIntent

    data object Back : MyAppealsIntent
}

/** 잔여 횟수를 담지 않는다 — 이의 횟수 한도가 폐기됐다(챌린지 정책 §7.2). */
data class MyAppealsState(
    val isLoading: Boolean,
    val history: List<AppealHistoryItem>,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyAppealsState(
                isLoading = true,
                history = emptyList(),
                errorMessage = null,
            )
    }
}

sealed interface MyAppealsReducerEvent : ReducerEvent {
    data object Loading : MyAppealsReducerEvent

    data class Loaded(
        val history: List<AppealHistoryItem>,
    ) : MyAppealsReducerEvent

    data class Failed(
        val message: String,
    ) : MyAppealsReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyAppealsEffect = NoEffect
