package com.ruleup.notification.presentation.center.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 알림 센터 ViewModel. */
@HiltViewModel
class NotificationCenterViewModel
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<NotificationCenterIntent, NotificationCenterState, NotificationCenterReducerEvent, NotificationCenterEffect>(
            NotificationCenterState.initial,
        ) {
        /** 이번 세션에서 읽음 처리를 이미 보낸 탭. */
        private val readTabs = mutableSetOf<NotificationTab>()

        override fun onIntent(intent: NotificationCenterIntent) {
            when (intent) {
                NotificationCenterIntent.Load -> load()

                is NotificationCenterIntent.SelectTab -> {
                    if (intent.tab == currentState.tab) return
                    dispatch(NotificationCenterReducerEvent.TabChanged(intent.tab))
                    load()
                }
                NotificationCenterIntent.LoadMore -> loadMore()

                is NotificationCenterIntent.Open -> {
                    val deeplink = intent.notification.deeplink
                    if (deeplink == null) {
                        // 갈 곳이 없는 알림도 있다(단순 고지)
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

                is NotificationCenterReducerEvent.TabChanged ->
                    NotificationCenterState.initial.copy(tab = event.tab)

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
            val tab = currentState.tab
            dispatch(NotificationCenterReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { notificationRepository.getNotifications(tab) }
                    .onSuccess { page ->
                        dispatch(
                            NotificationCenterReducerEvent.Loaded(
                                items = page.items,
                                unreadIds = page.unread.map { it.id }.toSet(),
                                nextCursor = page.nextCursor,
                                retentionDays = page.retentionDays,
                            ),
                        )
                        markRead(tab, page.readMarker)
                    }.onFailure {
                        dispatch(NotificationCenterReducerEvent.Failed(it.message ?: "알림을 불러오지 못했어요"))
                    }
            }
        }

        /** 읽음 처리. */
        private fun markRead(
            tab: NotificationTab,
            lastNotificationId: String?,
        ) {
            val marker = lastNotificationId ?: return
            if (!readTabs.add(tab)) return
            viewModelScope.launch {
                runCatching { notificationRepository.markRead(tab, marker) }
            }
        }

        private fun loadMore() {
            val cursor = currentState.nextCursor ?: return
            if (currentState.isLoadingMore) return
            val tab = currentState.tab
            dispatch(NotificationCenterReducerEvent.LoadingMore(true))
            viewModelScope.launch {
                runCatching { notificationRepository.getNotifications(tab, cursor) }
                    .onSuccess {
                        dispatch(NotificationCenterReducerEvent.MoreLoaded(it.items, it.nextCursor))
                    }.onFailure {
                        dispatch(NotificationCenterReducerEvent.LoadingMore(false))
                        emitEffect(NotificationCenterEffect.ShowMessage(it.message ?: "더 불러오지 못했어요"))
                    }
            }
        }
    }
