package com.ruleup.challenge.presentation.watchers.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.WATCHER_FREE_LIMIT
import com.ruleup.challenge.domain.entity.WatcherLimitExceededException
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.challenge.presentation.watcher.inviteCard
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 내 감시자 관리 ViewModel. */
@HiltViewModel
class ChallengeWatchersViewModel
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val watcherRepository: WatcherRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ChallengeWatchersIntent, ChallengeWatchersState, ChallengeWatchersReducerEvent, ChallengeWatchersEffect>(
            ChallengeWatchersState.initial,
        ) {
        override fun onIntent(intent: ChallengeWatchersIntent) {
            when (intent) {
                is ChallengeWatchersIntent.Load -> load(intent.challengeId)
                ChallengeWatchersIntent.Retry -> load(currentState.challengeId)
                ChallengeWatchersIntent.Invite -> invite()
                ChallengeWatchersIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ChallengeWatchersState,
            event: ChallengeWatchersReducerEvent,
        ): ChallengeWatchersState =
            when (event) {
                is ChallengeWatchersReducerEvent.Loading ->
                    state.copy(challengeId = event.challengeId, isLoading = true, errorMessage = null)

                is ChallengeWatchersReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        challengeTitle = event.challengeTitle,
                        watcherEnabled = event.watcherEnabled,
                        watchers = event.watchers,
                        errorMessage = null,
                    )

                is ChallengeWatchersReducerEvent.WatchersLoaded -> state.copy(watchers = event.watchers)

                is ChallengeWatchersReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)

                is ChallengeWatchersReducerEvent.Inviting -> state.copy(isInviting = event.inviting)
            }

        private fun load(challengeId: String) {
            if (challengeId.isBlank()) {
                dispatch(ChallengeWatchersReducerEvent.Failed(LOAD_FAILED_MESSAGE))
                return
            }
            dispatch(ChallengeWatchersReducerEvent.Loading(challengeId))
            viewModelScope.launch {
                runCatching {
                    val detail = challengeRepository.getChallenge(challengeId)
                    // 공개 상세가 penalties 를 안 줄 수 있다. 모르면 목록을 물어 받아지면 쓸 수 있는 방으로 본다
                    val watchers =
                        when (detail.penalties?.watcher) {
                            false -> null
                            true -> watcherRepository.getWatchers(challengeId)
                            null -> runCatching { watcherRepository.getWatchers(challengeId) }.getOrNull()
                        }
                    ChallengeWatchersReducerEvent.Loaded(detail.title, watchers != null, watchers)
                }.onSuccess { dispatch(it) }
                    .onFailure { dispatch(ChallengeWatchersReducerEvent.Failed(it.userFacingMessage(LOAD_FAILED_MESSAGE))) }
            }
        }

        /** 초대를 만들어 내 카카오톡으로 공유한다. */
        private fun invite() {
            val state = currentState
            if (state.isInviting || !state.watcherEnabled) return
            viewModelScope.launch {
                dispatch(ChallengeWatchersReducerEvent.Inviting(true))
                runCatching { watcherRepository.createInvitation(state.challengeId) }
                    .onSuccess { invitation ->
                        emitEffect(
                            ChallengeWatchersEffect.ShareInvite(
                                card = invitation.inviteCard(challengeTitle = state.challengeTitle.orEmpty()),
                                inviteUrl = invitation.inviteUrl,
                            ),
                        )
                        runCatching { watcherRepository.getWatchers(state.challengeId) }
                            .onSuccess { dispatch(ChallengeWatchersReducerEvent.WatchersLoaded(it)) }
                    }.onFailure { throwable ->
                        val message =
                            if (throwable is WatcherLimitExceededException) {
                                "무료 감시자 ${WATCHER_FREE_LIMIT}명을 모두 사용했어요. 구독하면 무제한으로 추가할 수 있어요"
                            } else {
                                throwable.userFacingMessage("감시자 초대에 실패했어요")
                            }
                        emitEffect(ChallengeWatchersEffect.ShowMessage(message))
                    }
                dispatch(ChallengeWatchersReducerEvent.Inviting(false))
            }
        }

        private companion object {
            const val LOAD_FAILED_MESSAGE = "감시자 정보를 불러오지 못했어요"
        }
    }
