package com.ruleup.challenge.presentation.watcher.invitation.viewmodel

import com.ruleup.challenge.domain.entity.WatcherInvitationInfo
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface WatcherInvitationIntent : MviIntent {
    /** 초대 딥링크 진입 — 토큰 검증 + 초대 정보 조회. */
    data class Load(
        val token: String,
    ) : WatcherInvitationIntent

    /** "수락하기" — 인앱 수락이 곧 수신동의(채널 = 인앱 푸시). */
    data object Accept : WatcherInvitationIntent

    data object Back : WatcherInvitationIntent
}

data class WatcherInvitationState(
    val isLoading: Boolean,
    val token: String,
    val info: WatcherInvitationInfo?,
    val isAccepting: Boolean,
    // 이번 진입에서 수락을 완료했는지(완료 화면 렌더)
    val accepted: Boolean,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            WatcherInvitationState(
                isLoading = true,
                token = "",
                info = null,
                isAccepting = false,
                accepted = false,
                errorMessage = null,
            )
    }
}

sealed interface WatcherInvitationReducerEvent : ReducerEvent {
    data class Loading(
        val token: String,
    ) : WatcherInvitationReducerEvent

    data class Loaded(
        val info: WatcherInvitationInfo,
    ) : WatcherInvitationReducerEvent

    data class Accepting(
        val accepting: Boolean,
    ) : WatcherInvitationReducerEvent

    data object Accepted : WatcherInvitationReducerEvent

    data class Failed(
        val message: String,
    ) : WatcherInvitationReducerEvent
}
