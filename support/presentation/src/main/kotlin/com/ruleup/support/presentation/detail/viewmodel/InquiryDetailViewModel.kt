package com.ruleup.support.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.navigation.InquiryCategoryPage
import com.ruleup.support.domain.repository.InquiryRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 문의 상세 ViewModel. */
@HiltViewModel
class InquiryDetailViewModel
    @Inject
    constructor(
        private val inquiryRepository: InquiryRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<InquiryDetailIntent, InquiryDetailState, InquiryDetailReducerEvent, NoEffect>(
            InquiryDetailState.initial,
        ) {
        override fun onIntent(intent: InquiryDetailIntent) {
            when (intent) {
                is InquiryDetailIntent.Load -> load(intent.inquiryId)
                InquiryDetailIntent.Retry -> load(currentState.inquiryId)
                InquiryDetailIntent.Back -> navigationHelper.navigateToBack()
                InquiryDetailIntent.NewInquiry -> navigationHelper.navigateTo(InquiryCategoryPage)
            }
        }

        override fun reduce(
            state: InquiryDetailState,
            event: InquiryDetailReducerEvent,
        ): InquiryDetailState =
            when (event) {
                is InquiryDetailReducerEvent.Loading ->
                    state.copy(inquiryId = event.inquiryId, isLoading = true, errorMessage = null)

                is InquiryDetailReducerEvent.Loaded ->
                    state.copy(isLoading = false, detail = event.detail, errorMessage = null)

                is InquiryDetailReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load(inquiryId: String) {
            if (inquiryId.isBlank()) {
                // 인자 없이 열린 화면이다.
                dispatch(InquiryDetailReducerEvent.Failed(NOT_FOUND_MESSAGE))
                return
            }
            dispatch(InquiryDetailReducerEvent.Loading(inquiryId))
            viewModelScope.launch {
                runCatching { inquiryRepository.getInquiry(inquiryId) }
                    .onSuccess { dispatch(InquiryDetailReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(InquiryDetailReducerEvent.Failed(it.userMessage())) }
            }
        }
    }

/** 남의 문의도 404 로 온다 */
private fun Throwable.userMessage(): String =
    when ((this as? InquiryException)?.failure) {
        InquiryFailure.NOT_FOUND -> NOT_FOUND_MESSAGE
        InquiryFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: "문의를 불러오지 못했어요"
    }

private const val NOT_FOUND_MESSAGE = "찾을 수 없는 문의예요"
