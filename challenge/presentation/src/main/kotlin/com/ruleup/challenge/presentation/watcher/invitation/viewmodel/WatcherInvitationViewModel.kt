package com.ruleup.challenge.presentation.watcher.invitation.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.usecase.AcceptWatcherInvitationUseCase
import com.ruleup.challenge.domain.usecase.GetWatcherInvitationUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 감시자 초대 수락 ViewModel. 카카오톡 초대 카드의 링크(딥링크)로 진입한다.
 * 토큰 상태(수락 가능/만료/이미 수락/30일 차단)에 따라 화면이 분기하고,
 * 룰업 유저의 인앱 수락이 곧 수신동의다(감시자 통지 스펙 §1).
 */
@HiltViewModel
class WatcherInvitationViewModel
    @Inject
    constructor(
        private val getWatcherInvitationUseCase: GetWatcherInvitationUseCase,
        private val acceptWatcherInvitationUseCase: AcceptWatcherInvitationUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<WatcherInvitationIntent, WatcherInvitationState, WatcherInvitationReducerEvent, NoEffect>(
            WatcherInvitationState.initial,
        ) {
        override fun onIntent(intent: WatcherInvitationIntent) {
            when (intent) {
                is WatcherInvitationIntent.Load -> load(intent.token)
                WatcherInvitationIntent.Accept -> accept()
                WatcherInvitationIntent.OpenChallenge -> openChallenge()
                WatcherInvitationIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: WatcherInvitationState,
            event: WatcherInvitationReducerEvent,
        ): WatcherInvitationState =
            when (event) {
                is WatcherInvitationReducerEvent.Loading ->
                    state.copy(isLoading = true, token = event.token, errorMessage = null)

                is WatcherInvitationReducerEvent.Loaded ->
                    state.copy(isLoading = false, info = event.info, errorMessage = null)

                is WatcherInvitationReducerEvent.Accepting -> state.copy(isAccepting = event.accepting)

                WatcherInvitationReducerEvent.Accepted ->
                    state.copy(isAccepting = false, accepted = true)

                is WatcherInvitationReducerEvent.Failed ->
                    state.copy(isLoading = false, isAccepting = false, errorMessage = event.message)
            }

        private fun load(token: String) {
            if (currentState.token == token && currentState.info != null) return
            viewModelScope.launch {
                dispatch(WatcherInvitationReducerEvent.Loading(token))
                runCatching { getWatcherInvitationUseCase(token) }
                    .onSuccess { dispatch(WatcherInvitationReducerEvent.Loaded(it)) }
                    .onFailure {
                        dispatch(WatcherInvitationReducerEvent.Failed(it.message ?: "초대 정보를 불러오지 못했어요"))
                    }
            }
        }

        private fun accept() {
            if (currentState.isAccepting) return
            val token = currentState.token.ifBlank { return }
            viewModelScope.launch {
                dispatch(WatcherInvitationReducerEvent.Accepting(true))
                runCatching { acceptWatcherInvitationUseCase(token) }
                    .onSuccess { dispatch(WatcherInvitationReducerEvent.Accepted) }
                    .onFailure {
                        dispatch(WatcherInvitationReducerEvent.Failed(it.message ?: "초대 수락에 실패했어요"))
                    }
            }
        }

        private fun openChallenge() {
            val challengeId = currentState.info?.challengeId ?: return
            navigationHelper.navigateByRoute(ChallengeDetailPage(challengeId).toRoute())
        }
    }
