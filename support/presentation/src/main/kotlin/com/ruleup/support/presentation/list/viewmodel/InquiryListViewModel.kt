package com.ruleup.support.presentation.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.navigation.InquiryCategoryPage
import com.ruleup.support.domain.navigation.InquiryDetailPage
import com.ruleup.support.domain.repository.InquiryReadStore
import com.ruleup.support.domain.repository.InquiryRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 내 문의 내역 ViewModel. */
@HiltViewModel
class InquiryListViewModel
    @Inject
    constructor(
        private val inquiryRepository: InquiryRepository,
        private val inquiryReadStore: InquiryReadStore,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<InquiryListIntent, InquiryListState, InquiryListReducerEvent, NoEffect>(
            InquiryListState.initial,
        ) {
        override fun onIntent(intent: InquiryListIntent) {
            when (intent) {
                InquiryListIntent.Load, InquiryListIntent.Retry -> load()
                InquiryListIntent.Back -> navigationHelper.navigateToBack()
                is InquiryListIntent.Open -> navigationHelper.navigateTo(InquiryDetailPage(intent.inquiryId))
                InquiryListIntent.NewInquiry -> navigationHelper.navigateTo(InquiryCategoryPage)
            }
        }

        override fun reduce(
            state: InquiryListState,
            event: InquiryListReducerEvent,
        ): InquiryListState =
            when (event) {
                InquiryListReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is InquiryListReducerEvent.Loaded ->
                    state.copy(isLoading = false, items = event.items, seenAnswers = event.seenAnswers, errorMessage = null)

                is InquiryListReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            dispatch(InquiryListReducerEvent.Loading)
            viewModelScope.launch {
                runCatching {
                    coroutineScope {
                        // 기록을 못 읽으면 전부 새 답변으로 보인다. 목록을 막을 일은 아니다
                        val seen = async { runCatching { inquiryReadStore.seenAnswers() }.getOrDefault(emptyMap()) }
                        inquiryRepository.getInquiries() to seen.await()
                    }
                }.onSuccess { (items, seen) ->
                    dispatch(InquiryListReducerEvent.Loaded(items, seen))
                }.onFailure { dispatch(InquiryListReducerEvent.Failed(it.userMessage())) }
            }
        }
    }

private fun Throwable.userMessage(): String =
    when ((this as? InquiryException)?.failure) {
        InquiryFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: "문의 내역을 불러오지 못했어요"
    }
