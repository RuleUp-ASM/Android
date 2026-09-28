package com.ruleup.profile.presentation.locked.viewmodel

import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface AccountLockedIntent : MviIntent {
    data object Load : AccountLockedIntent

    data object Retry : AccountLockedIntent

    /** 제재 이력. */
    data object OpenHistory : AccountLockedIntent

    data object OpenNotifications : AccountLockedIntent

    /** 재검토. */
    data object RequestReview : AccountLockedIntent

    data object Logout : AccountLockedIntent
}

data class AccountLockedState(
    val isLoading: Boolean,
    val sanction: ActiveSanction?,
    val errorMessage: String?,
    val isOffline: Boolean,
) : UiState {
    companion object {
        val initial =
            AccountLockedState(
                isLoading = true,
                sanction = null,
                errorMessage = null,
                isOffline = false,
            )
    }
}

sealed interface AccountLockedReducerEvent : ReducerEvent {
    data object Loading : AccountLockedReducerEvent

    data class Loaded(
        val sanction: ActiveSanction?,
    ) : AccountLockedReducerEvent

    data class Failed(
        val message: String,
        val offline: Boolean,
    ) : AccountLockedReducerEvent
}

/** 이동은 전부 NavigationHelper 로 나가고 화면에 남는 일회성 알림이 없다. */
typealias AccountLockedEffect = NoEffect
