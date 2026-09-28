package com.ruleup.support.presentation.detail.viewmodel

import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryDetailIntent : MviIntent {
    /** 화면 진입. */
    data class Load(
        val inquiryId: String,
    ) : InquiryDetailIntent

    data object Retry : InquiryDetailIntent

    data object Back : InquiryDetailIntent

    /** 같은 사안을 다시 묻는 유일한 경로. */
    data object NewInquiry : InquiryDetailIntent
}

data class InquiryDetailState(
    // 재시도가 같은 문의를 다시 부르려면 상태가 기억해야 한다.
    val inquiryId: String,
    val isLoading: Boolean,
    val detail: InquiryDetail?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial = InquiryDetailState(inquiryId = "", isLoading = true, detail = null, errorMessage = null)
    }
}

sealed interface InquiryDetailReducerEvent : ReducerEvent {
    data class Loading(
        val inquiryId: String,
    ) : InquiryDetailReducerEvent

    data class Loaded(
        val detail: InquiryDetail,
    ) : InquiryDetailReducerEvent

    data class Failed(
        val message: String,
    ) : InquiryDetailReducerEvent
}

typealias InquiryDetailEffect = NoEffect
