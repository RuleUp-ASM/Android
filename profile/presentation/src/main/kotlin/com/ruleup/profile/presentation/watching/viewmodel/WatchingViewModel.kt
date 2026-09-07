package com.ruleup.profile.presentation.watching.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.AlreadyRevokedException
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 패널티 수신 관리 ViewModel (Figma 1134:2221).
 *
 * 두 동작이 명확히 다르다 — **푸시 토글**은 관계를 끊지 않고 푸시만 멈추며 알림함 적재는 유지되고,
 * **완전 수신거부**는 관계를 REVOKED 로 바꾸고 같은 생성자의 재초대를 30일간 막는다. 서버가 둘의
 * 동시 전송을 막으므로 요청도 한 번에 하나만 보낸다.
 *
 * 상태는 **서버 응답으로만** 갱신한다 — 동의·철회 시각이 증거라 낙관적 반영을 하지 않는다.
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
                is WatchingIntent.TogglePush -> togglePush(intent.watcherId, intent.enabled)
                is WatchingIntent.ConfirmRevoke -> dispatch(WatchingReducerEvent.RevokeTargetChanged(intent.watcherId))
                WatchingIntent.DismissRevoke -> dispatch(WatchingReducerEvent.RevokeTargetChanged(null))
                WatchingIntent.Revoke -> revoke()
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

                is WatchingReducerEvent.Updating -> state.copy(updating = event.watcherId)

                is WatchingReducerEvent.ItemUpdated ->
                    state.copy(
                        items =
                            state.items.map {
                                if (it.watcherId == event.watcherId) it.copy(pushEnabled = event.pushEnabled) else it
                            },
                    )

                is WatchingReducerEvent.ItemRevoked ->
                    // 거부한 관계는 목록에서 뺀다 — 토글이 남아 있으면 다시 켤 수 있어 보인다.
                    state.copy(items = state.items.filterNot { it.watcherId == event.watcherId })

                is WatchingReducerEvent.RevokeTargetChanged -> state.copy(revokeTarget = event.watcherId)
            }

        private fun load() {
            dispatch(WatchingReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { watcherRepository.getWatching() }
                    // 이미 거부한 관계는 서버가 내려줘도 목록에 세우지 않는다 — 되돌릴 방법이 없다.
                    .onSuccess { list -> dispatch(WatchingReducerEvent.Loaded(list.filterNot { it.isRevoked })) }
                    .onFailure { dispatch(WatchingReducerEvent.Failed(it.message ?: "감시 목록을 불러오지 못했어요")) }
            }
        }

        private fun togglePush(
            watcherId: String,
            enabled: Boolean,
        ) {
            if (currentState.updating != null) return
            dispatch(WatchingReducerEvent.Updating(watcherId))
            viewModelScope.launch {
                runCatching { watcherRepository.updateWatching(watcherId, pushEnabled = enabled) }
                    .onSuccess { dispatch(WatchingReducerEvent.ItemUpdated(watcherId, it.pushEnabled)) }
                    .onFailure { emitEffect(WatchingEffect.ShowMessage(it.message ?: "설정을 바꾸지 못했어요")) }
                dispatch(WatchingReducerEvent.Updating(null))
            }
        }

        private fun revoke() {
            val watcherId = currentState.revokeTarget ?: return
            if (currentState.updating != null) return
            dispatch(WatchingReducerEvent.Updating(watcherId))
            viewModelScope.launch {
                runCatching { watcherRepository.updateWatching(watcherId, revoke = true) }
                    .onSuccess {
                        dispatch(WatchingReducerEvent.ItemRevoked(watcherId))
                        emitEffect(WatchingEffect.ShowMessage("더 이상 이 알림을 받지 않아요"))
                    }.onFailure {
                        // 이미 거부된 항목이면 목록에서 빼는 게 맞는 결과다 — 실패로 안내하지 않는다.
                        if (it is AlreadyRevokedException) {
                            dispatch(WatchingReducerEvent.ItemRevoked(watcherId))
                        } else {
                            emitEffect(WatchingEffect.ShowMessage(it.message ?: "수신을 거부하지 못했어요"))
                        }
                    }
                dispatch(WatchingReducerEvent.Updating(null))
                dispatch(WatchingReducerEvent.RevokeTargetChanged(null))
            }
        }
    }
