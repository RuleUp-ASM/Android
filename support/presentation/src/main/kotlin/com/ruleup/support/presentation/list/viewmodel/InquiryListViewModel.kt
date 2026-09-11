package com.ruleup.support.presentation.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.navigation.InquiryCategoryPage
import com.ruleup.support.domain.navigation.InquiryDetailPage
import com.ruleup.support.domain.repository.InquiryRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 내 문의 내역 ViewModel (Figma `1419:27`, 명세 GET /api/v1/inquiries).
 *
 * 화면에 들어올 때마다 다시 부른다 — 운영자가 답변을 달아도 앱은 알림을 받지 않으므로(2026-09-11
 * 결정) 이 조회가 새 답변을 알아채는 유일한 경로다. 캐시를 두면 답변이 왔는데 옛 목록이 남는다.
 */
@HiltViewModel
class InquiryListViewModel
    @Inject
    constructor(
        private val inquiryRepository: InquiryRepository,
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
                    state.copy(isLoading = false, items = event.items, errorMessage = null)

                is InquiryListReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            dispatch(InquiryListReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { inquiryRepository.getInquiries() }
                    .onSuccess { dispatch(InquiryListReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(InquiryListReducerEvent.Failed(it.userMessage())) }
            }
        }
    }

private fun Throwable.userMessage(): String =
    when ((this as? InquiryException)?.failure) {
        InquiryFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: "문의 내역을 불러오지 못했어요"
    }
