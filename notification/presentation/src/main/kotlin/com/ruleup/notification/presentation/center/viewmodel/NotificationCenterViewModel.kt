package com.ruleup.notification.presentation.center.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 센터 ViewModel.
 *
 * 두 가지가 이 화면의 계약이다.
 * - **읽음 처리는 첫 페이지 조회 직후 한 번뿐이다.** 커서 페이징에서 부르면 뒤 페이지의 더 작은
 *   id 로 읽음 지점이 과거로 밀린다.
 * - 보내는 값은 **응답에 실제로 담겼던 최신 id** 다. 서버가 현재 시각으로 갱신하면 조회와 갱신
 *   사이에 적재된 알림이 화면에 뜬 적 없이 읽음 처리되어 레드닷이 영영 안 뜬다.
 *
 * 개별 읽음 API 는 없다 — 항목을 눌러도 읽음 상태는 바뀌지 않는다.
 */
@HiltViewModel
class NotificationCenterViewModel
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<NotificationCenterIntent, NotificationCenterState, NotificationCenterReducerEvent, NotificationCenterEffect>(
            NotificationCenterState.initial,
        ) {
        override fun onIntent(intent: NotificationCenterIntent) {
            when (intent) {
                NotificationCenterIntent.Load -> load()
                NotificationCenterIntent.LoadMore -> loadMore()

                is NotificationCenterIntent.Open -> {
                    val deeplink = intent.notification.deeplink
                    if (deeplink == null) {
                        // 갈 곳이 없는 알림도 있다(단순 고지) — 아무 일도 하지 않는 게 맞다.
                        return
                    }
                    emitEffect(NotificationCenterEffect.OpenDeeplink(deeplink))
                }

                NotificationCenterIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: NotificationCenterState,
            event: NotificationCenterReducerEvent,
        ): NotificationCenterState =
            when (event) {
                NotificationCenterReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is NotificationCenterReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        items = event.items,
                        unreadIds = event.unreadIds,
                        nextCursor = event.nextCursor,
                        retentionDays = event.retentionDays,
                        errorMessage = null,
                    )

                is NotificationCenterReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is NotificationCenterReducerEvent.LoadingMore -> state.copy(isLoadingMore = event.loading)

                is NotificationCenterReducerEvent.MoreLoaded ->
                    state.copy(
                        isLoadingMore = false,
                        items = state.items + event.items,
                        nextCursor = event.nextCursor,
                    )
            }

        private fun load() {
            if (!currentState.isLoading && currentState.items.isNotEmpty()) return
            dispatch(NotificationCenterReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { notificationRepository.getNotifications() }
                    .onSuccess { page ->
                        dispatch(
                            NotificationCenterReducerEvent.Loaded(
                                items = page.items,
                                unreadIds = page.unread.map { it.id }.toSet(),
                                nextCursor = page.nextCursor,
                                retentionDays = page.retentionDays,
                            ),
                        )
                        markRead(page.readMarker)
                    }.onFailure {
                        dispatch(NotificationCenterReducerEvent.Failed(it.message ?: "알림을 불러오지 못했어요"))
                    }
            }
        }

        /**
         * 읽음 처리. 실패해도 화면에는 알리지 않는다 — 사용자가 할 수 있는 일이 없고, 다음 진입에서
         * 다시 시도된다. 대신 레드닷이 한 번 더 뜰 뿐이다.
         */
        private fun markRead(lastNotificationId: String?) {
            val marker = lastNotificationId ?: return
            viewModelScope.launch {
                runCatching { notificationRepository.markRead(NotificationTab.NOTIFICATION, marker) }
            }
        }

        private fun loadMore() {
            val cursor = currentState.nextCursor ?: return
            if (currentState.isLoadingMore) return
            dispatch(NotificationCenterReducerEvent.LoadingMore(true))
            viewModelScope.launch {
                runCatching { notificationRepository.getNotifications(cursor) }
                    .onSuccess {
                        dispatch(NotificationCenterReducerEvent.MoreLoaded(it.items, it.nextCursor))
                    }.onFailure {
                        dispatch(NotificationCenterReducerEvent.LoadingMore(false))
                        emitEffect(NotificationCenterEffect.ShowMessage(it.message ?: "더 불러오지 못했어요"))
                    }
            }
        }
    }
