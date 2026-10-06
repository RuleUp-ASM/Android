package com.ruleup.challenge.presentation.watchers.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeWatchersIntent : MviIntent {
    data class Load(
        val challengeId: String,
    ) : ChallengeWatchersIntent

    data object Retry : ChallengeWatchersIntent

    data object Invite : ChallengeWatchersIntent

    data object Back : ChallengeWatchersIntent
}

data class ChallengeWatchersState(
    val challengeId: String,
    val isLoading: Boolean,
    val challengeTitle: String?,
    // 이 방이 감시자 벌칙을 쓰는가. 꺼져 있으면 목록 대신 안내를 그린다
    val watcherEnabled: Boolean,
    val watchers: ChallengeWatchers?,
    val isInviting: Boolean,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            ChallengeWatchersState(
                challengeId = "",
                isLoading = true,
                challengeTitle = null,
                watcherEnabled = false,
                watchers = null,
                isInviting = false,
                errorMessage = null,
            )
    }
}

sealed interface ChallengeWatchersReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
    ) : ChallengeWatchersReducerEvent

    data class Loaded(
        val challengeTitle: String,
        val watcherEnabled: Boolean,
        val watchers: ChallengeWatchers?,
    ) : ChallengeWatchersReducerEvent

    data class WatchersLoaded(
        val watchers: ChallengeWatchers,
    ) : ChallengeWatchersReducerEvent

    data class Failed(
        val message: String,
    ) : ChallengeWatchersReducerEvent

    data class Inviting(
        val inviting: Boolean,
    ) : ChallengeWatchersReducerEvent
}

sealed interface ChallengeWatchersEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ChallengeWatchersEffect

    data class ShareInvite(
        val card: WatcherInviteCard,
        val inviteUrl: String,
    ) : ChallengeWatchersEffect
}
