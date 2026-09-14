package com.ruleup.support.presentation.detail.viewmodel

import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryDetailIntent : MviIntent {
    /**
     * 화면 진입. **인자를 여기로 받는다** — 이 내비게이션은 `SavedStateHandle` 을 채우지 않아
     * ViewModel 이 라우트 인자를 직접 읽을 수 없다.
     */
    data class Load(
        val inquiryId: String,
    ) : InquiryDetailIntent

    data object Retry : InquiryDetailIntent

    data object Back : InquiryDetailIntent

    /**
     * 같은 사안을 다시 묻는 유일한 경로. **재문의가 아니라 새 문의다** — 답변 뒤 같은 스레드에
     * 글을 더하는 계약이 없어(명세) 분류부터 다시 고른다.
     */
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
