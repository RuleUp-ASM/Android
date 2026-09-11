package com.ruleup.support.presentation.compose.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.support.domain.entity.InquiryBody
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.navigation.InquiryComposePage
import com.ruleup.support.domain.repository.InquiryRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 문의하기 · 작성 ViewModel (Figma `1417:117`, 명세 POST /api/v1/inquiries).
 *
 * **접수는 되돌릴 수 없다.** 수정·삭제 경로가 없고 재시도는 같은 내용을 한 건 더 쌓으면서 하루
 * 상한을 함께 깎는다. 그래서 응답이 오기 전에는 버튼을 잠근다.
 */
@HiltViewModel
class InquiryComposeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val inquiryRepository: InquiryRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<InquiryComposeIntent, InquiryComposeState, InquiryComposeReducerEvent, NoEffect>(
            InquiryComposeState.initial(
                // 인자가 없거나 모르는 값이면 기타로 둔다 — 분류 없이 폼을 띄우면 접수가 400 으로 막힌다.
                InquiryCategory.fromValue(savedStateHandle[InquiryComposePage.ARG_CATEGORY])
                    ?: InquiryCategory.ERROR_ETC,
            ),
        ) {
        override fun onIntent(intent: InquiryComposeIntent) {
            when (intent) {
                InquiryComposeIntent.Back -> navigationHelper.navigateToBack()
                InquiryComposeIntent.ChangeCategory -> navigationHelper.navigateToBack()
                is InquiryComposeIntent.BodyChanged -> dispatch(InquiryComposeReducerEvent.BodyEdited(intent.value))
                is InquiryComposeIntent.ImagePicked -> upload(intent.uri)
                is InquiryComposeIntent.ImageRemoved -> dispatch(InquiryComposeReducerEvent.ImageRemoved(intent.uri))
                InquiryComposeIntent.Submit -> submit()
                InquiryComposeIntent.ConfirmReceipt -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: InquiryComposeState,
            event: InquiryComposeReducerEvent,
        ): InquiryComposeState =
            when (event) {
                is InquiryComposeReducerEvent.BodyEdited ->
                    // 입력을 자르지 않는다 — 붙여넣기가 조용히 잘리면 사용자가 지워진 걸 모른다.
                    // 상한 초과는 글자 수 표기가 빨개지고 버튼이 잠기는 것으로 알린다.
                    state.copy(body = event.value, errorMessage = null)

                is InquiryComposeReducerEvent.ImageAdded ->
                    state.copy(
                        attachments = state.attachments + InquiryAttachment(uri = event.uri),
                        errorMessage = null,
                    )

                is InquiryComposeReducerEvent.ImageUploaded ->
                    state.copy(
                        attachments =
                            state.attachments.map {
                                if (it.uri == event.uri) it.copy(uploadedUrl = event.url) else it
                            },
                    )

                is InquiryComposeReducerEvent.ImageFailed ->
                    state.copy(
                        attachments =
                            state.attachments.map {
                                if (it.uri == event.uri) it.copy(failed = true) else it
                            },
                        errorMessage = event.message,
                    )

                is InquiryComposeReducerEvent.ImageRemoved ->
                    state.copy(
                        attachments = state.attachments.filterNot { it.uri == event.uri },
                        errorMessage = null,
                    )

                InquiryComposeReducerEvent.Submitting -> state.copy(submitting = true, errorMessage = null)

                is InquiryComposeReducerEvent.Submitted ->
                    state.copy(submitting = false, receiptId = event.inquiryId, errorMessage = null)

                is InquiryComposeReducerEvent.SubmitFailed ->
                    state.copy(submitting = false, errorMessage = event.message)
            }

        private fun upload(uri: String) {
            if (!currentState.canAddImage) return
            dispatch(InquiryComposeReducerEvent.ImageAdded(uri))
            viewModelScope.launch {
                runCatching { inquiryRepository.uploadImage(uri) }
                    .onSuccess { dispatch(InquiryComposeReducerEvent.ImageUploaded(uri, it)) }
                    .onFailure {
                        dispatch(
                            InquiryComposeReducerEvent.ImageFailed(uri, it.userMessage("사진을 올리지 못했어요")),
                        )
                    }
            }
        }

        private fun submit() {
            val state = currentState
            if (!state.canSubmit) return
            val submission =
                runCatching {
                    InquirySubmission(
                        category = state.category,
                        body = InquiryBody.of(state.body),
                        // 실패한 장은 빼고 보낸다 — 주소가 없어 서버가 받을 수 없다.
                        imageUrls = state.attachments.mapNotNull { it.uploadedUrl },
                    )
                }.getOrElse {
                    dispatch(InquiryComposeReducerEvent.SubmitFailed(it.message ?: "문의 내용을 다시 확인해 주세요"))
                    return
                }

            dispatch(InquiryComposeReducerEvent.Submitting)
            viewModelScope.launch {
                runCatching { inquiryRepository.submit(submission) }
                    .onSuccess { dispatch(InquiryComposeReducerEvent.Submitted(it.inquiryId)) }
                    .onFailure {
                        dispatch(InquiryComposeReducerEvent.SubmitFailed(it.userMessage("문의를 접수하지 못했어요")))
                    }
            }
        }
    }

/**
 * 실패를 사용자 문구로 옮긴다.
 *
 * 하루 상한과 길이 초과는 **서버 문구를 그대로 쓴다** — 남은 건수나 초과 글자 수처럼 서버만 아는
 * 숫자가 문장에 들어 있어, 앱이 다시 쓰면 그 숫자가 빠진 밋밋한 안내가 된다.
 */
private fun Throwable.userMessage(fallback: String): String =
    when ((this as? InquiryException)?.failure) {
        InquiryFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        InquiryFailure.IMAGE_REJECTED -> "이 사진은 올릴 수 없어요. 다른 사진을 골라 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
