package com.ruleup.support.presentation.compose.viewmodel

import com.ruleup.support.domain.entity.InquiryBody
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryLimits
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryComposeIntent : MviIntent {
    /** 화면 진입. */
    data class Load(
        val category: InquiryCategory,
    ) : InquiryComposeIntent

    data object Back : InquiryComposeIntent

    /** 분류를 고르러 되돌아간다. */
    data object ChangeCategory : InquiryComposeIntent

    data class BodyChanged(
        val value: String,
    ) : InquiryComposeIntent

    /** 갤러리에서 고른 로컬 URI. */
    data class ImagePicked(
        val uri: String,
    ) : InquiryComposeIntent

    data class ImageRemoved(
        val uri: String,
    ) : InquiryComposeIntent

    data class RetryImage(
        val uri: String,
    ) : InquiryComposeIntent

    data object Submit : InquiryComposeIntent

    /** 접수 완료 시트의 확인. */
    data object ConfirmReceipt : InquiryComposeIntent
}

/** 첨부 사진 한 장의 상태. */
data class InquiryAttachment(
    val uri: String,
    val uploadedUrl: String? = null,
    val failed: Boolean = false,
) {
    val uploading: Boolean
        get() = uploadedUrl == null && !failed
}

/** 작성 화면 상태. */
data class InquiryComposeState(
    val category: InquiryCategory,
    val body: String,
    val attachments: List<InquiryAttachment>,
    val submitting: Boolean,
    val errorMessage: String?,
    // 접수에 성공하면 채워지고 시트가 뜬다.
    val receiptId: String?,
) : UiState {
    val bodyLength: Int
        get() = body.trim().length

    /** 접수 버튼을 열 수 있는가. */
    val canSubmit: Boolean
        get() =
            !submitting &&
                receiptId == null &&
                InquiryBody.isValid(body) &&
                attachments.none { it.uploading } &&
                attachments.count { it.uploadedUrl != null } <= InquiryLimits.IMAGE_MAX_COUNT

    val canAddImage: Boolean
        get() = attachments.size < InquiryLimits.IMAGE_MAX_COUNT

    companion object {
        // 분류는 진입 직후 Load 가 채운다.
        val initial = initial(InquiryCategory.ERROR_ETC)

        fun initial(category: InquiryCategory) =
            InquiryComposeState(
                category = category,
                body = "",
                attachments = emptyList(),
                submitting = false,
                errorMessage = null,
                receiptId = null,
            )
    }
}

sealed interface InquiryComposeReducerEvent : ReducerEvent {
    data class CategoryLoaded(
        val category: InquiryCategory,
    ) : InquiryComposeReducerEvent

    data class BodyEdited(
        val value: String,
    ) : InquiryComposeReducerEvent

    data class ImageRetrying(
        val uri: String,
    ) : InquiryComposeReducerEvent

    data class ImageAdded(
        val uri: String,
    ) : InquiryComposeReducerEvent

    data class ImageUploaded(
        val uri: String,
        val url: String,
    ) : InquiryComposeReducerEvent

    data class ImageFailed(
        val uri: String,
        val message: String,
    ) : InquiryComposeReducerEvent

    data class ImageRemoved(
        val uri: String,
    ) : InquiryComposeReducerEvent

    data object Submitting : InquiryComposeReducerEvent

    data class Submitted(
        val inquiryId: String,
    ) : InquiryComposeReducerEvent

    data class SubmitFailed(
        val message: String,
    ) : InquiryComposeReducerEvent
}

/** 일회성 이펙트 없음. */
typealias InquiryComposeEffect = NoEffect
