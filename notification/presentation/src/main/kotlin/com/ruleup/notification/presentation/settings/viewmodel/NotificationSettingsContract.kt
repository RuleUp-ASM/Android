package com.ruleup.notification.presentation.settings.viewmodel

import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface NotificationSettingsIntent : MviIntent {
    data object Load : NotificationSettingsIntent

    data class ToggleMaster(
        val enabled: Boolean,
    ) : NotificationSettingsIntent

    data class ToggleGroup(
        val group: NotificationGroup,
        val enabled: Boolean,
    ) : NotificationSettingsIntent

    /** OS 권한 배너 → 앱 설정 화면. */
    data object OpenSystemSettings : NotificationSettingsIntent

    data object Back : NotificationSettingsIntent
}

sealed interface NotificationSettingsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : NotificationSettingsEffect

    /** OS 앱 설정으로 보낸다 */
    data object OpenSystemSettings : NotificationSettingsEffect
}

data class NotificationSettingsState(
    val isLoading: Boolean,
    val settings: NotificationSettings?,
    // 전송 중인 토글.
    val submitting: Boolean,
    /** OS 푸시 권한이 꺼져 있는가. */
    val systemPermissionDenied: Boolean,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            NotificationSettingsState(
                isLoading = true,
                settings = null,
                submitting = false,
                systemPermissionDenied = false,
                errorMessage = null,
            )
    }
}

sealed interface NotificationSettingsReducerEvent : ReducerEvent {
    data object Loading : NotificationSettingsReducerEvent

    data class Loaded(
        val settings: NotificationSettings,
    ) : NotificationSettingsReducerEvent

    data class Failed(
        val message: String,
    ) : NotificationSettingsReducerEvent

    data class Submitting(
        val submitting: Boolean,
    ) : NotificationSettingsReducerEvent

    data class PermissionChecked(
        val denied: Boolean,
    ) : NotificationSettingsReducerEvent
}
