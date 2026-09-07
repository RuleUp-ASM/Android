package com.ruleup.profile.presentation.watching.viewmodel

import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface WatchingIntent : MviIntent {
    data object Load : WatchingIntent

    /** 푸시만 끄기 — 관계는 유지되고 알림함 적재도 그대로다. */
    data class TogglePush(
        val watcherId: String,
        val enabled: Boolean,
    ) : WatchingIntent

    /** 완전 수신거부 확인 시트 열기. 30일 재초대 차단이 걸리므로 한 번 묻는다. */
    data class ConfirmRevoke(
        val watcherId: String,
    ) : WatchingIntent

    data object DismissRevoke : WatchingIntent

    data object Revoke : WatchingIntent

    data object Back : WatchingIntent
}

sealed interface WatchingEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : WatchingEffect
}

data class WatchingState(
    val isLoading: Boolean,
    val items: List<Watching>,
    // 전송 중인 항목. 응답이 올 때까지 그 행만 잠근다
    val updating: String?,
    // 수신거부 확인 시트 대상 (null = 닫힘)
    val revokeTarget: String?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            WatchingState(
                isLoading = true,
                items = emptyList(),
                updating = null,
                revokeTarget = null,
                errorMessage = null,
            )
    }
}

sealed interface WatchingReducerEvent : ReducerEvent {
    data object Loading : WatchingReducerEvent

    data class Loaded(
        val items: List<Watching>,
    ) : WatchingReducerEvent

    data class Failed(
        val message: String,
    ) : WatchingReducerEvent

    data class Updating(
        val watcherId: String?,
    ) : WatchingReducerEvent

    /** 서버가 받아들인 값으로 그 행만 갈아 끼운다 — 전체를 다시 받지 않는다. */
    data class ItemUpdated(
        val watcherId: String,
        val pushEnabled: Boolean,
    ) : WatchingReducerEvent

    data class ItemRevoked(
        val watcherId: String,
    ) : WatchingReducerEvent

    data class RevokeTargetChanged(
        val watcherId: String?,
    ) : WatchingReducerEvent
}
