package com.ruleup.profile.presentation.settings.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface SettingsIntent : MviIntent {
    data object Load : SettingsIntent

    /** 내가 받는 알림 관리 — 내가 감시자로 지정된 관계의 수신 설정. */
    data object OpenWatching : SettingsIntent

    data object OpenBlocks : SettingsIntent

    data object OpenAgreements : SettingsIntent

    data object OpenSanctions : SettingsIntent

    /** 알림 설정 — 서버 미완이라 안내만 한다. */
    data object OpenNotificationSettings : SettingsIntent

    data object ConfirmLogout : SettingsIntent

    data object ConfirmWithdraw : SettingsIntent

    /** 확인 시트에서 실제로 실행. */
    data object Logout : SettingsIntent

    data object Withdraw : SettingsIntent

    data object DismissDialog : SettingsIntent

    data object Back : SettingsIntent
}

sealed interface SettingsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : SettingsEffect
}

/** 확인이 필요한 되돌릴 수 없는 동작. 둘 다 시트를 한 번 거친다. */
enum class SettingsDialog {
    LOGOUT,
    WITHDRAW,
}

data class SettingsState(
    val isLoading: Boolean,
    // 재동의가 필요한 약관이 있으면 「약관 · 개인정보」 행에 표시한다
    val reconsentCount: Int,
    // 효력 중인 제재가 있으면 「제재 이력」 행에 표시한다
    val hasActiveSanction: Boolean,
    val dialog: SettingsDialog?,
    val isSubmitting: Boolean,
) : UiState {
    companion object {
        val initial =
            SettingsState(
                isLoading = true,
                reconsentCount = 0,
                hasActiveSanction = false,
                dialog = null,
                isSubmitting = false,
            )
    }
}

sealed interface SettingsReducerEvent : ReducerEvent {
    data class Loaded(
        val reconsentCount: Int,
        val hasActiveSanction: Boolean,
    ) : SettingsReducerEvent

    data object LoadFinished : SettingsReducerEvent

    data class DialogShown(
        val dialog: SettingsDialog,
    ) : SettingsReducerEvent

    data object DialogDismissed : SettingsReducerEvent

    data class Submitting(
        val submitting: Boolean,
    ) : SettingsReducerEvent
}
