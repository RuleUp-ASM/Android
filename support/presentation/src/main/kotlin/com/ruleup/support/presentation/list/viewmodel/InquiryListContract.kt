package com.ruleup.support.presentation.list.viewmodel

import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryListIntent : MviIntent {
    data object Load : InquiryListIntent

    data object Retry : InquiryListIntent

    data object Back : InquiryListIntent

    data class Open(
        val inquiryId: String,
    ) : InquiryListIntent

    data object NewInquiry : InquiryListIntent
}

data class InquiryListState(
    val isLoading: Boolean,
    val items: List<InquirySummary>,
    val errorMessage: String?,
    // 문의 id → 이 기기에서 확인한 답변 시각
    val seenAnswers: Map<String, String> = emptyMap(),
) : UiState {
    val isEmpty: Boolean
        get() = items.isEmpty()

    fun hasNewAnswer(item: InquirySummary): Boolean = item.hasNewAnswer(seenAnswers[item.inquiryId])

    companion object {
        val initial = InquiryListState(isLoading = true, items = emptyList(), errorMessage = null)
    }
}

sealed interface InquiryListReducerEvent : ReducerEvent {
    data object Loading : InquiryListReducerEvent

    data class Loaded(
        val items: List<InquirySummary>,
        val seenAnswers: Map<String, String>,
    ) : InquiryListReducerEvent

    data class Failed(
        val message: String,
    ) : InquiryListReducerEvent
}

typealias InquiryListEffect = NoEffect
