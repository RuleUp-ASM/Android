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
) : UiState {
    val isEmpty: Boolean
        get() = items.isEmpty()

    companion object {
        val initial = InquiryListState(isLoading = true, items = emptyList(), errorMessage = null)
    }
}

sealed interface InquiryListReducerEvent : ReducerEvent {
    data object Loading : InquiryListReducerEvent

    data class Loaded(
        val items: List<InquirySummary>,
    ) : InquiryListReducerEvent

    data class Failed(
        val message: String,
    ) : InquiryListReducerEvent
}

typealias InquiryListEffect = NoEffect
