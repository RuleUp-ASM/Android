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

    /** 이의 제기 제출(실패 일자에 대한 재검토 요청). */
    data class SubmitObjection(
        val targetDate: String,
        val content: String,
    ) : VerificationDetailIntent
}

data class VerificationDetailState(
    val isLoading: Boolean = false,
    val detail: VerificationDetail? = null,
    val error: String? = null,
    // 이의 제기 제출 중(버튼 중복 탭 방지).
    val isSubmittingObjection: Boolean = false,
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

    /** 이의 제기 제출 시작/종료. */
    data class SubmittingObjection(
        val submitting: Boolean,
    ) : VerificationDetailReducerEvent
}

sealed interface VerificationDetailEffect : MviEffect {
    /** 권한 설정 화면 열기(명세 §6.4 CTA). */
    data object OpenPermissionSettings : VerificationDetailEffect

    /** 이의 제기 접수/실패 등 안내 메시지. */
    data class ShowMessage(
        val message: String,
    ) : VerificationDetailEffect
}
