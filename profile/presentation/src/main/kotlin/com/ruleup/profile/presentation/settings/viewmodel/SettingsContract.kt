package com.ruleup.profile.presentation.settings.viewmodel

import com.ruleup.domain.entity.user.SocialProvider
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

    data object OpenNotificationSettings : SettingsIntent

    data object OpenNotificationCenter : SettingsIntent

    /** 문의하기 — 분류 선택부터 시작한다. */
    data object OpenInquiry : SettingsIntent

    data object OpenInquiryHistory : SettingsIntent

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
    // 연결된 소셜 제공자. 모르면 null 이라 「연결된 계정」 행이 제공자 이름 없이 그려진다
    val provider: SocialProvider?,
    // 재동의가 필요한 약관이 있으면 「약관 · 개인정보」 행에 표시한다
    val reconsentCount: Int,
    // 효력 중인 제재가 있으면 「제재 이력」 행에 표시한다
    val hasActiveSanction: Boolean,
    /**
     * 답변이 달린 문의 수. 「내 문의 내역」 행의 뱃지다.
     *
     * 문의 답변은 푸시도 알림함도 쓰지 않기로 해(2026-09-11) **이 숫자가 답변을 알리는 유일한
     * 신호**다. 0 이면 뱃지를 그리지 않는다.
     */
    val answeredInquiryCount: Int,
    val dialog: SettingsDialog?,
    val isSubmitting: Boolean,
) : UiState {
    companion object {
        val initial =
            SettingsState(
                isLoading = true,
                provider = null,
                reconsentCount = 0,
                hasActiveSanction = false,
                answeredInquiryCount = 0,
                dialog = null,
                isSubmitting = false,
            )
    }
}

sealed interface SettingsReducerEvent : ReducerEvent {
    data class Loaded(
        val provider: SocialProvider?,
        val reconsentCount: Int,
        val hasActiveSanction: Boolean,
        val answeredInquiryCount: Int,
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
