package com.ruleup.support.presentation.detail.viewmodel

import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryDetailIntent : MviIntent {
    data object Load : InquiryDetailIntent

    data object Retry : InquiryDetailIntent

    data object Back : InquiryDetailIntent

    /**
     * 같은 사안을 다시 묻는 유일한 경로. **재문의가 아니라 새 문의다** — 답변 뒤 같은 스레드에
     * 글을 더하는 계약이 없어(명세) 분류부터 다시 고른다.
     */
    data object NewInquiry : InquiryDetailIntent
}

data class InquiryDetailState(
    val isLoading: Boolean,
    val detail: InquiryDetail?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial = InquiryDetailState(isLoading = true, detail = null, errorMessage = null)
    }
}

sealed interface InquiryDetailReducerEvent : ReducerEvent {
    data object Loading : InquiryDetailReducerEvent

    data class Loaded(
        val detail: InquiryDetail,
    ) : InquiryDetailReducerEvent

    data class Failed(
        val message: String,
    ) : InquiryDetailReducerEvent
}

typealias InquiryDetailEffect = NoEffect
