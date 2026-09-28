package com.ruleup.notification.presentation.center.viewmodel

import com.ruleup.notification.domain.entity.Notification
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface NotificationCenterIntent : MviIntent {
    data object Load : NotificationCenterIntent

    /** 탭 전환. */
    data class SelectTab(
        val tab: NotificationTab,
    ) : NotificationCenterIntent

    /** 목록 끝에 닿음 */
    data object LoadMore : NotificationCenterIntent

    data class Open(
        val notification: Notification,
    ) : NotificationCenterIntent

    data object Back : NotificationCenterIntent
}

sealed interface NotificationCenterEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : NotificationCenterEffect

    /** 알림이 가리키는 곳으로 이동. */
    data class OpenDeeplink(
        val deeplink: String,
    ) : NotificationCenterEffect
}

data class NotificationCenterState(
    /** 지금 보고 있는 탭. */
    val tab: NotificationTab,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val items: List<Notification>,
    /** 진입 시점에 미읽음이던 항목 id. */
    val unreadIds: Set<String>,
    val nextCursor: String?,
    val retentionDays: Int?,
    val errorMessage: String?,
) : UiState {
    val hasMore: Boolean
        get() = nextCursor != null

    companion object {
        val initial =
            NotificationCenterState(
                tab = NotificationTab.NOTIFICATION,
                isLoading = true,
                isLoadingMore = false,
                items = emptyList(),
                unreadIds = emptySet(),
                nextCursor = null,
                retentionDays = null,
                errorMessage = null,
            )
    }
}

sealed interface NotificationCenterReducerEvent : ReducerEvent {
    data object Loading : NotificationCenterReducerEvent

    /** 탭이 바뀌었다. */
    data class TabChanged(
        val tab: NotificationTab,
    ) : NotificationCenterReducerEvent

    data class Loaded(
        val items: List<Notification>,
        val unreadIds: Set<String>,
        val nextCursor: String?,
        val retentionDays: Int?,
    ) : NotificationCenterReducerEvent

    data class Failed(
        val message: String,
    ) : NotificationCenterReducerEvent

    data class LoadingMore(
        val loading: Boolean,
    ) : NotificationCenterReducerEvent

    data class MoreLoaded(
        val items: List<Notification>,
        val nextCursor: String?,
    ) : NotificationCenterReducerEvent
}
