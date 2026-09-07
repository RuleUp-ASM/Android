package com.ruleup.challenge.presentation.watcher.viewmodel

import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface WatcherAcceptIntent : MviIntent {
    /** 화면 진입 — 아직 수락하지 않는다. 무엇에 동의하는지 먼저 보여 준다. */
    data class Load(
        val token: String,
    ) : WatcherAcceptIntent

    data object Accept : WatcherAcceptIntent

    data object GoHome : WatcherAcceptIntent

    data object Back : WatcherAcceptIntent
}

/**
 * 수락 실패 사유. 다음에 할 일이 서로 달라 하나의 오류 문구로 접지 않는다 —
 * 만료는 "다시 초대해 달라고 하기", 중복은 "이미 됐다", 본인 수락은 "안 되는 일"이다.
 */
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

/** 결과를 화면 안에서 보여 주고 이동은 버튼으로만 한다 — 단발성 이펙트가 없다. */
typealias WatcherAcceptEffect = NoEffect
