package com.ruleup.challenge.presentation.watcher.viewmodel

import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface WatcherAcceptIntent : MviIntent {
    /** 화면 진입 */
    data class Load(
        val token: String,
    ) : WatcherAcceptIntent

    data object Accept : WatcherAcceptIntent

    data object GoHome : WatcherAcceptIntent

    data object Back : WatcherAcceptIntent
}

/** 수락 실패 사유. */
enum class WatcherAcceptFailure {
    EXPIRED,
    ALREADY_ACCEPTED,
    SELF,
    BLOCKED,
    INVALID,
    UNKNOWN,
}

data class WatcherAcceptState(
    val isSubmitting: Boolean,
    val accepted: WatcherAcceptance?,
    val failure: WatcherAcceptFailure?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            WatcherAcceptState(
                isSubmitting = false,
                accepted = null,
                failure = null,
                errorMessage = null,
            )
    }
}

sealed interface WatcherAcceptReducerEvent : ReducerEvent {
    data class Submitting(
        val submitting: Boolean,
    ) : WatcherAcceptReducerEvent

    data class Accepted(
        val acceptance: WatcherAcceptance,
    ) : WatcherAcceptReducerEvent

    data class Failed(
        val failure: WatcherAcceptFailure,
        val message: String,
    ) : WatcherAcceptReducerEvent
}

/** 결과를 화면 안에서 보여 주고 이동은 버튼으로만 한다 */
typealias WatcherAcceptEffect = NoEffect
