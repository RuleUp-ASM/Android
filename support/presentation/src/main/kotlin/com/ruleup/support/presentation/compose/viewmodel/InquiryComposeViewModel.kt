package com.ruleup.support.presentation.compose.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.support.domain.entity.InquiryBody
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.repository.InquiryRepository
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 문의하기 · 작성 ViewModel. */
@HiltViewModel
class InquiryComposeViewModel
    @Inject
    constructor(
        private val inquiryRepository: InquiryRepository,
        private val navigationHelper: NavigationHelper,
        private val savedStateHandle: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle(),
    ) : MviViewModel<InquiryComposeIntent, InquiryComposeState, InquiryComposeReducerEvent, NoEffect>(
            InquiryComposeState.initial,
        ) {
        init {
            savedStateHandle.get<String>("inquiryBody")?.let { dispatch(InquiryComposeReducerEvent.BodyEdited(it)) }
        }

        override fun onIntent(intent: InquiryComposeIntent) {
            when (intent) {
                is InquiryComposeIntent.Load -> dispatch(InquiryComposeReducerEvent.CategoryLoaded(intent.category))
                InquiryComposeIntent.Back -> navigationHelper.navigateToBack()
                InquiryComposeIntent.ChangeCategory -> navigationHelper.navigateToBack()
                is InquiryComposeIntent.BodyChanged -> {
                    savedStateHandle["inquiryBody"] = intent.value
                    dispatch(InquiryComposeReducerEvent.BodyEdited(intent.value))
                }
                is InquiryComposeIntent.RetryImage -> upload(intent.uri, retry = true)
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
                is InquiryComposeReducerEvent.CategoryLoaded -> state.copy(category = event.category)

                is InquiryComposeReducerEvent.BodyEdited ->
                    // 문의 본문.
                    state.copy(body = event.value, errorMessage = null)

                is InquiryComposeReducerEvent.ImageRetrying ->
                    state.copy(
                        attachments =
                            state.attachments.map {
                                if (it.uri == event.uri) it.copy(failed = false) else it
                            },
                        errorMessage = null,
                    )

                is InquiryComposeReducerEvent.ImageAdded ->
                    state.copy(
                        attachments = state.attachments + InquiryAttachment(uri = event.uri),
                        errorMessage = null,
                    )

                is InquiryComposeReducerEvent.ImageUploaded ->
                    state.copy(
                        attachments =
                            state.attachments.map {
                                if (it.uri == event.uri) it.copy(uploadedUrl = event.url, failed = false) else it
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

        private fun upload(
            uri: String,
            retry: Boolean = false,
        ) {
            if (retry) {
                if (currentState.attachments.none { it.uri == uri && it.failed }) return
                dispatch(InquiryComposeReducerEvent.ImageRetrying(uri))
            } else {
                if (!currentState.canAddImage || currentState.attachments.any { it.uri == uri }) return
                dispatch(InquiryComposeReducerEvent.ImageAdded(uri))
            }
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
                        // 실패한 장은 빼고 보낸다
                        imageUrls = state.attachments.mapNotNull { it.uploadedUrl },
                    )
                }.getOrElse {
                    dispatch(InquiryComposeReducerEvent.SubmitFailed(it.userFacingMessage("문의 내용을 다시 확인해 주세요")))
                    return
                }

            dispatch(InquiryComposeReducerEvent.Submitting)
            viewModelScope.launch {
                runCatching { inquiryRepository.submit(submission) }
                    .onSuccess {
                        savedStateHandle.remove<String>("inquiryBody")
                        dispatch(InquiryComposeReducerEvent.Submitted(it.inquiryId))
                    }.onFailure {
                        dispatch(InquiryComposeReducerEvent.SubmitFailed(it.userMessage("문의를 접수하지 못했어요")))
                    }
            }
        }
    }

/** 실패를 사용자 문구로 옮긴다. */
private fun Throwable.userMessage(fallback: String): String =
    when ((this as? InquiryException)?.failure) {
        InquiryFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        InquiryFailure.IMAGE_REJECTED -> "이 사진은 올릴 수 없어요. 다른 사진을 골라 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
