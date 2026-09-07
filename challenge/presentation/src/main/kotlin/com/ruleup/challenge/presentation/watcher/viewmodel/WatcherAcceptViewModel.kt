package com.ruleup.challenge.presentation.watcher.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.AlreadyConsentedException
import com.ruleup.challenge.domain.entity.CannotWatchSelfException
import com.ruleup.challenge.domain.entity.InvitationExpiredException
import com.ruleup.challenge.domain.entity.InvitationInvalidException
import com.ruleup.challenge.domain.entity.WatcherBlockedException
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 감시자 초대 수락 ViewModel.
 *
 * **자동으로 수락하지 않는다** — 수락이 곧 개인정보 수신 동의라, 링크를 연 것만으로 동의가 성립하면
 * 안 된다. 화면이 무엇에 동의하는지 보여 주고 사용자가 누를 때만 보낸다.
 */
@HiltViewModel
class WatcherAcceptViewModel
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<WatcherAcceptIntent, WatcherAcceptState, WatcherAcceptReducerEvent, NoEffect>(
            WatcherAcceptState.initial,
        ) {
        // 라우트 인자는 화면이 Load 로 넘겨준다(레지스트리가 args 맵을 화면에 준다).
        private var token: String = ""

        override fun onIntent(intent: WatcherAcceptIntent) {
            when (intent) {
                // 토큰이 아예 없으면 누를 것도 없다 — 들어온 순간 사유를 보여 준다.
                is WatcherAcceptIntent.Load -> {
                    token = intent.token
                    if (token.isBlank()) {
                        dispatch(
                            WatcherAcceptReducerEvent.Failed(
                                WatcherAcceptFailure.INVALID,
                                InvitationInvalidException().message.orEmpty(),
                            ),
                        )
                    }
                }

                WatcherAcceptIntent.Accept -> accept()
                WatcherAcceptIntent.GoHome -> navigationHelper.replaceStackWith(NavRoute(AppRoutes.HOME))
                WatcherAcceptIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: WatcherAcceptState,
            event: WatcherAcceptReducerEvent,
        ): WatcherAcceptState =
            when (event) {
                is WatcherAcceptReducerEvent.Submitting -> state.copy(isSubmitting = event.submitting)

                is WatcherAcceptReducerEvent.Accepted ->
                    state.copy(isSubmitting = false, accepted = event.acceptance, failure = null, errorMessage = null)

                is WatcherAcceptReducerEvent.Failed ->
                    state.copy(isSubmitting = false, failure = event.failure, errorMessage = event.message)
            }

        private fun accept() {
            if (currentState.isSubmitting || token.isBlank()) return
            dispatch(WatcherAcceptReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { watcherRepository.acceptInvitation(token) }
                    .onSuccess { dispatch(WatcherAcceptReducerEvent.Accepted(it)) }
                    .onFailure {
                        dispatch(
                            WatcherAcceptReducerEvent.Failed(
                                it.toFailure(),
                                it.message ?: "초대를 수락하지 못했어요",
                            ),
                        )
                    }
            }
        }
    }

private fun Throwable.toFailure(): WatcherAcceptFailure =
    when (this) {
        is InvitationExpiredException -> WatcherAcceptFailure.EXPIRED
        is AlreadyConsentedException -> WatcherAcceptFailure.ALREADY_ACCEPTED
        is CannotWatchSelfException -> WatcherAcceptFailure.SELF
        is WatcherBlockedException -> WatcherAcceptFailure.BLOCKED
        is InvitationInvalidException -> WatcherAcceptFailure.INVALID
        else -> WatcherAcceptFailure.UNKNOWN
    }
