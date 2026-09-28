package com.ruleup.challenge.presentation.invite.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 멤버 초대 링크 진입 ViewModel. */
@HiltViewModel
class ChallengeInviteViewModel
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ChallengeInviteIntent, ChallengeInviteState, ChallengeInviteReducerEvent, ChallengeInviteEffect>(
            ChallengeInviteState.initial,
        ) {
        private var token: String = ""

        override fun onIntent(intent: ChallengeInviteIntent) {
            when (intent) {
                is ChallengeInviteIntent.Load -> {
                    token = intent.token
                    load()
                }

                ChallengeInviteIntent.Accept -> accept()
                ChallengeInviteIntent.GoHome -> navigationHelper.replaceStackWith(NavRoute(AppRoutes.HOME))
                ChallengeInviteIntent.Back -> navigationHelper.replaceStackWith(NavRoute(AppRoutes.HOME))
            }
        }

        override fun reduce(
            state: ChallengeInviteState,
            event: ChallengeInviteReducerEvent,
        ): ChallengeInviteState =
            when (event) {
                ChallengeInviteReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is ChallengeInviteReducerEvent.Loaded ->
                    state.copy(isLoading = false, preview = event.preview, errorMessage = null)

                is ChallengeInviteReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)

                is ChallengeInviteReducerEvent.Accepting -> state.copy(isAccepting = event.accepting)

                is ChallengeInviteReducerEvent.Blocked -> state.copy(isAccepting = false, blockedBy = event.reason)
            }

        private fun load() {
            if (token.isBlank()) {
                dispatch(ChallengeInviteReducerEvent.Failed("초대 링크를 확인할 수 없어요"))
                return
            }
            dispatch(ChallengeInviteReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { challengeRepository.getInvitation(token) }
                    .onSuccess {
                        dispatch(ChallengeInviteReducerEvent.Loaded(it))
                        if (it.blockReason == com.ruleup.challenge.domain.entity.JoinBlockReason.ALREADY_JOINED) {
                            navigationHelper.replaceStackWith(ChallengeDetailPage(it.challenge.challengeId).toRoute())
                        }
                    }.onFailure { dispatch(ChallengeInviteReducerEvent.Failed(it.userFacingMessage("초대를 불러오지 못했어요"))) }
            }
        }

        private fun accept() {
            val preview = currentState.preview ?: return
            if (!currentState.canAccept) return
            dispatch(ChallengeInviteReducerEvent.Accepting(true))
            viewModelScope.launch {
                runCatching { challengeRepository.acceptInvitation(token) }
                    .onSuccess {
                        dispatch(ChallengeInviteReducerEvent.Accepting(false))
                        // 이미 쓴 토큰으로 돌아오지 않도록 스택을 방 상세로 갈아 끼운다.
                        navigationHelper.replaceStackWith(
                            ChallengeDetailPage(preview.challenge.challengeId).toRoute(),
                        )
                    }.onFailure { failure ->
                        if (failure is JoinBlockedException) {
                            dispatch(ChallengeInviteReducerEvent.Blocked(failure.reason))
                        } else {
                            dispatch(ChallengeInviteReducerEvent.Accepting(false))
                            emitEffect(ChallengeInviteEffect.ShowMessage(failure.message ?: "참여하지 못했어요"))
                        }
                    }
            }
        }
    }
