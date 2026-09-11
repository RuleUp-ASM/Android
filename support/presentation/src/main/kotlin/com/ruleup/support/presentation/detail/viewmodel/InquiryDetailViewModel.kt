package com.ruleup.support.presentation.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
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
 * 문의 상세 ViewModel (Figma `1419:87`, 명세 GET /api/v1/inquiries/{inquiryId}).
 *
 * 열람 전용이다 — 답변에 이어 쓰는 경로가 계약에 없어 이 화면이 보낼 요청은 조회 하나뿐이다.
 */
@HiltViewModel
class InquiryDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val inquiryRepository: InquiryRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<InquiryDetailIntent, InquiryDetailState, InquiryDetailReducerEvent, NoEffect>(
            InquiryDetailState.initial,
        ) {
        private val inquiryId: String = savedStateHandle[InquiryDetailPage.ARG_INQUIRY_ID] ?: ""

        override fun onIntent(intent: InquiryDetailIntent) {
            when (intent) {
                InquiryDetailIntent.Load, InquiryDetailIntent.Retry -> load()
                InquiryDetailIntent.Back -> navigationHelper.navigateToBack()
                InquiryDetailIntent.NewInquiry -> navigationHelper.navigateTo(InquiryCategoryPage)
            }
        }

        override fun reduce(
            state: InquiryDetailState,
            event: InquiryDetailReducerEvent,
        ): InquiryDetailState =
            when (event) {
                InquiryDetailReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is InquiryDetailReducerEvent.Loaded ->
                    state.copy(isLoading = false, detail = event.detail, errorMessage = null)

                is InquiryDetailReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (inquiryId.isBlank()) {
                // 인자 없이 열린 화면이다. 조회를 보내 봐야 404 라 서버를 부르지 않는다.
                dispatch(InquiryDetailReducerEvent.Failed(NOT_FOUND_MESSAGE))
                return
            }
            dispatch(InquiryDetailReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { inquiryRepository.getInquiry(inquiryId) }
                    .onSuccess { dispatch(InquiryDetailReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(InquiryDetailReducerEvent.Failed(it.userMessage())) }
            }
        }
    }

/**
 * 남의 문의도 404 로 온다 — 서버가 존재 여부를 흘리지 않으려고 일부러 구분하지 않는다.
 * 화면도 같은 문구를 쓴다. "권한이 없어요"라고 말하면 그 번호의 문의가 있다는 뜻이 된다.
 */
private fun Throwable.userMessage(): String =
    when ((this as? InquiryException)?.failure) {
        InquiryFailure.NOT_FOUND -> NOT_FOUND_MESSAGE
        InquiryFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: "문의를 불러오지 못했어요"
    }

private const val NOT_FOUND_MESSAGE = "찾을 수 없는 문의예요"
