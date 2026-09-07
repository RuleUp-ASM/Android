package com.ruleup.notification.presentation.center.viewmodel

import com.ruleup.notification.domain.entity.Notification
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface NotificationCenterIntent : MviIntent {
    data object Load : NotificationCenterIntent

    /** 목록 끝에 닿음 — 다음 페이지가 있으면 이어 읽는다. */
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

    /**
     * 알림이 가리키는 곳으로 이동. 딥링크 해석은 `:app` 이 한다 —
     * feature 가 앱 전체의 라우트 표를 알 이유가 없다.
     */
    data class OpenDeeplink(
        val deeplink: String,
    ) : NotificationCenterEffect
}

data class NotificationCenterState(
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val items: List<Notification>,
    /**
     * 진입 시점에 미읽음이던 항목 id.
     *
     * **진입 직후 읽음 처리를 보내지만 이 표시는 세션 내내 유지한다** — 눈앞에서 점이 사라지면
     * 사용자는 무엇이 새로 온 것이었는지 알 수 없다.
     */
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
