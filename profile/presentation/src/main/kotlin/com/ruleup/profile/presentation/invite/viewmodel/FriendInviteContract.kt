package com.ruleup.profile.presentation.invite.viewmodel

import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface FriendInviteIntent : MviIntent {
    /** 화면 진입 시 초대 정보 조회 (코드 없으면 서버가 생성 — 멱등). */
    data object Load : FriendInviteIntent

    /** 카카오톡 공유 (사용자 본인 발신). */
    data object ShareKakao : FriendInviteIntent

    data object CopyLink : FriendInviteIntent

    data object Back : FriendInviteIntent
}

sealed interface FriendInviteEffect : MviEffect {
    /** 카카오톡 공유 실행 (Context 필요 — 화면이 수행). */
    data class LaunchKakaoShare(
        val inviteUrl: String,
        val inviteCode: String,
    ) : FriendInviteEffect

    /** 링크 클립보드 복사 (화면이 수행). */
    data class CopyToClipboard(
        val inviteUrl: String,
    ) : FriendInviteEffect

    data class ShowMessage(
        val message: String,
    ) : FriendInviteEffect
}

data class FriendInviteState(
    val isLoading: Boolean,
    val invitation: FriendInvitation?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            FriendInviteState(
                isLoading = true,
                invitation = null,
                errorMessage = null,
            )
    }
}

sealed interface FriendInviteReducerEvent : ReducerEvent {
    data object Loading : FriendInviteReducerEvent

    data class Loaded(
        val invitation: FriendInvitation,
    ) : FriendInviteReducerEvent

    data class Failed(
        val message: String,
    ) : FriendInviteReducerEvent
}
