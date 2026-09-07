package com.ruleup.challenge.presentation.invite.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 멤버 초대 링크 진입 ViewModel.
 *
 * 미리보기와 수락이 갈려 있다 — 조회는 토큰을 소모하지 않고, 수락에서만 소모된다. 그래서
 * **들어온 것만으로 가입시키지 않는다**: 어떤 방인지 보여 주고 누를 때만 가입한다.
 *
 * 수락에 성공하면 **백스택을 방 상세로 교체**한다. 초대 화면으로 되돌아가면 이미 쓴 토큰으로
 * 다시 수락을 시도하게 된다.
 */
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
                ChallengeInviteIntent.Back -> navigationHelper.navigateToBack()
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
                    .onSuccess { dispatch(ChallengeInviteReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(ChallengeInviteReducerEvent.Failed(it.message ?: "초대를 불러오지 못했어요")) }
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
