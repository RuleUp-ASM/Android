package com.ruleup.verification.presentation.detail.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.VerificationDetail

sealed interface VerificationDetailIntent : MviIntent {
    data class Load(
        val challengeId: String,
    ) : VerificationDetailIntent

    data object CtaClicked : VerificationDetailIntent
}

data class VerificationDetailState(
    val isLoading: Boolean = false,
    val detail: VerificationDetail? = null,
    val error: String? = null,
) : UiState {
    companion object {
        val initial = VerificationDetailState()
    }
}

sealed interface VerificationDetailReducerEvent : ReducerEvent {
    data object Loading : VerificationDetailReducerEvent

    data class Loaded(
        val detail: VerificationDetail,
    ) : VerificationDetailReducerEvent

    data class Failed(
        val message: String,
    ) : VerificationDetailReducerEvent
}

sealed interface VerificationDetailEffect : MviEffect {
    /** 권한 설정 화면 열기(명세 §6.4 CTA). */
    data object OpenPermissionSettings : VerificationDetailEffect
}
