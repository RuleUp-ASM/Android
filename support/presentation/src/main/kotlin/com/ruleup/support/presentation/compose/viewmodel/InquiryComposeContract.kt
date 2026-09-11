package com.ruleup.support.presentation.compose.viewmodel

import com.ruleup.support.domain.entity.InquiryBody
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryLimits
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface InquiryComposeIntent : MviIntent {
    data object Back : InquiryComposeIntent

    /** 분류를 고르러 되돌아간다. 화면 안에서 바꾸지 않는 이유는 [InquiryComposeState] KDoc 참고. */
    data object ChangeCategory : InquiryComposeIntent

    data class BodyChanged(
        val value: String,
    ) : InquiryComposeIntent

    /** 갤러리에서 고른 로컬 URI. 업로드는 접수 시점이 아니라 고른 즉시 시작한다. */
    data class ImagePicked(
        val uri: String,
    ) : InquiryComposeIntent

    data class ImageRemoved(
        val uri: String,
    ) : InquiryComposeIntent

    data object Submit : InquiryComposeIntent

    /** 접수 완료 시트의 확인. 시트를 닫고 설정 허브로 돌아간다. */
    data object ConfirmReceipt : InquiryComposeIntent
}

/**
 * 첨부 사진 한 장의 상태.
 *
 * 고른 즉시 업로드하고 결과를 [uploadedUrl] 에 담는다 — 접수 버튼에서 한꺼번에 올리면 3장이
 * 순차 업로드되는 동안 사용자가 멈춘 화면을 본다. 실패한 장은 [failed] 로 남겨 다시 고르게 한다.
 */
data class InquiryAttachment(
    val uri: String,
    val uploadedUrl: String? = null,
    val failed: Boolean = false,
) {
    val uploading: Boolean
        get() = uploadedUrl == null && !failed
}

/**
 * 작성 화면 상태.
 *
 * [category] 는 라우트 인자로 받아 고정된다 — 화면 안에서 바꾸면 분류만 바꾸고 본문은 그대로 둔
 * 채 되돌아오는 경로가 생겨, 본문과 분류가 어긋난 접수가 쌓인다. 바꾸려면 분류 화면으로 돌아간다.
 */
data class InquiryComposeState(
    val category: InquiryCategory,
    val body: String,
    val attachments: List<InquiryAttachment>,
    val submitting: Boolean,
    val errorMessage: String?,
    // 접수에 성공하면 채워지고 시트가 뜬다. 접수번호는 이 안에만 있다.
    val receiptId: String?,
) : UiState {
    val bodyLength: Int
        get() = body.trim().length

    /**
     * 접수 버튼을 열 수 있는가.
     *
     * 업로드가 끝나지 않은 사진이 있으면 막는다 — 주소 없이 보내면 그 장은 사라진다.
     * 이미 접수번호를 받았어도 막는다 — 접수는 되돌릴 수 없고, 두 번째 요청은 같은 문의를 한 건
     * 더 쌓으면서 하루 상한까지 깎는다.
     */
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
    data class BodyEdited(
        val value: String,
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

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias InquiryComposeEffect = NoEffect
