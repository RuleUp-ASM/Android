package com.ruleup.profile.presentation.invite.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 친구 초대 ViewModel. 초대 전달은 사용자 본인 채널(카카오톡·복사·QR)로만 — 룰업 직접 발송 금지.
 * 딥링크 앱 라우팅은 초대 경로 확정 후 별도 작업 — 여기서는 서버가 준 URL 을 그대로 표시·공유한다.
 */
@HiltViewModel
class FriendInviteViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<FriendInviteIntent, FriendInviteState, FriendInviteReducerEvent, FriendInviteEffect>(
            FriendInviteState.initial,
        ) {
        override fun onIntent(intent: FriendInviteIntent) {
            when (intent) {
                FriendInviteIntent.Load -> load()
                FriendInviteIntent.ShareKakao ->
                    currentState.invitation?.let {
                        emitEffect(FriendInviteEffect.LaunchKakaoShare(inviteUrl = it.inviteUrl, inviteCode = it.inviteCode))
                    }

                FriendInviteIntent.CopyLink ->
                    currentState.invitation?.let {
                        emitEffect(FriendInviteEffect.CopyToClipboard(it.inviteUrl))
                    }

                FriendInviteIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: FriendInviteState,
            event: FriendInviteReducerEvent,
        ): FriendInviteState =
            when (event) {
                FriendInviteReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is FriendInviteReducerEvent.Loaded ->
                    state.copy(isLoading = false, invitation = event.invitation, errorMessage = null)

                is FriendInviteReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.invitation != null) return
            dispatch(FriendInviteReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getInvitation() }
                    .onSuccess { dispatch(FriendInviteReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(FriendInviteReducerEvent.Failed(it.message ?: "초대 정보를 불러오지 못했어요")) }
            }
        }
    }
