package com.ruleup.challenge.presentation.invite.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeInvitationPreview
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeInviteIntent : MviIntent {
    data class Load(
        val token: String,
    ) : ChallengeInviteIntent

    data object Accept : ChallengeInviteIntent

    data object GoHome : ChallengeInviteIntent

    data object Back : ChallengeInviteIntent
}

sealed interface ChallengeInviteEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ChallengeInviteEffect
}

data class ChallengeInviteState(
    val isLoading: Boolean,
    val preview: ChallengeInvitationPreview?,
    val isAccepting: Boolean,
    // 수락을 눌러 막힌 사유.
    val blockedBy: JoinBlockReason?,
    val errorMessage: String?,
) : UiState {
    /** 지금 수락 버튼을 눌러도 되는가. */
    val canAccept: Boolean
        get() = preview?.joinable == true && !isAccepting

    companion object {
        val initial =
            ChallengeInviteState(
                isLoading = true,
                preview = null,
                isAccepting = false,
                blockedBy = null,
                errorMessage = null,
            )
    }
}

sealed interface ChallengeInviteReducerEvent : ReducerEvent {
    data object Loading : ChallengeInviteReducerEvent

    data class Loaded(
        val preview: ChallengeInvitationPreview,
    ) : ChallengeInviteReducerEvent

    data class Failed(
        val message: String,
    ) : ChallengeInviteReducerEvent

    data class Accepting(
        val accepting: Boolean,
    ) : ChallengeInviteReducerEvent

    data class Blocked(
        val reason: JoinBlockReason?,
    ) : ChallengeInviteReducerEvent
}
