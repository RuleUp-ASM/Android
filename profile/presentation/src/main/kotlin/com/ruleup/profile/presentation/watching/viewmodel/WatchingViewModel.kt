package com.ruleup.profile.presentation.watching.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 「내가 받는 알림」 ViewModel (Figma 1134:2221). **조회 전용**이다 — 관계 해제·관계별 수신 설정은
 * 폐지됐고(패널티 감시자 기능 스펙), 푸시는 알림 설정에서 켜고 끈다.
 */
@HiltViewModel
class WatchingViewModel
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<WatchingIntent, WatchingState, WatchingReducerEvent, WatchingEffect>(
            WatchingState.initial,
        ) {
        override fun onIntent(intent: WatchingIntent) {
            when (intent) {
                WatchingIntent.Load -> load()
                WatchingIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: WatchingState,
            event: WatchingReducerEvent,
        ): WatchingState =
            when (event) {
                WatchingReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is WatchingReducerEvent.Loaded ->
                    state.copy(isLoading = false, items = event.items, errorMessage = null)

                is WatchingReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            dispatch(WatchingReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { watcherRepository.getWatching() }
                    // 이미 거부된 관계는 서버가 내려줘도 목록에 세우지 않는다 — 더는 알림이 오지 않는다.
                    .onSuccess { list -> dispatch(WatchingReducerEvent.Loaded(list.filterNot { it.isRevoked })) }
                    .onFailure { dispatch(WatchingReducerEvent.Failed(it.message ?: "감시 목록을 불러오지 못했어요")) }
            }
        }
    }
