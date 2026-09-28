package com.ruleup.support.data.dto

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.requireField
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.entity.InquirySummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 접수

@Serializable
data class InquiryReceiptResponse(
    @SerialName("inquiryId")
    val inquiryId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

internal fun InquiryReceiptResponse.toDomain(): InquiryReceipt =
    InquiryReceipt(
        // 접수번호가 없으면 사용자도 CS 도 이 문의를 특정할 수 없다
        inquiryId = inquiryId.requireField("inquiryId"),
        status = InquiryStatus.fromValue(status),
        createdAt = createdAt.orEmpty(),
    )

// 목록

@Serializable
data class InquiryListResponse(
    @SerialName("items")
    val items: List<InquirySummaryResponse>? = null,
)

@Serializable
data class InquirySummaryResponse(
    @SerialName("inquiryId")
    val inquiryId: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("preview")
    val preview: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("answeredAt")
    val answeredAt: String? = null,
)

internal fun InquiryListResponse.toDomain(): List<InquirySummary> = items.orEmpty().map { it.toDomain() }

/** id 가 비면 터뜨린다 */
internal fun InquirySummaryResponse.toDomain(): InquirySummary =
    InquirySummary(
        inquiryId = inquiryId.requireField("inquiries.items[].inquiryId"),
        category = InquiryCategory.fromValue(category),
        status = InquiryStatus.fromValue(status),
        preview = preview.orEmpty(),
        createdAt = createdAt.orEmpty(),
        answeredAt = answeredAt,
    )

// 상세

@Serializable
data class InquiryDetailResponse(
    @SerialName("inquiryId")
    val inquiryId: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("imageUrls")
    val imageUrls: List<String>? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("answerText")
    val answerText: String? = null,
    @SerialName("answeredAt")
    val answeredAt: String? = null,
)

internal fun InquiryDetailResponse.toDomain(): InquiryDetail =
    InquiryDetail(
        inquiryId = inquiryId.requireField("inquiryId"),
        category = InquiryCategory.fromValue(category),
        status = InquiryStatus.fromValue(status),
        body = body.orEmpty(),
        imageUrls = imageUrls.orEmpty(),
        createdAt = createdAt.orEmpty(),
        answerText = answerText,
        answeredAt = answeredAt,
    )

// 사진 업로드

@Serializable
data class InquiryImageResponse(
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

// 에러 코드 → 화면 어휘

/** 서버 에러 코드를 화면이 아는 실패로 옮긴다. */
internal fun ApiException.toInquiryFailure(): InquiryFailure =
    when (code) {
        "INQUIRY_DAILY_LIMIT" -> InquiryFailure.DAILY_LIMIT
        "INQUIRY_BODY_LENGTH" -> InquiryFailure.BODY_LENGTH
        "INQUIRY_IMAGE_LIMIT" -> InquiryFailure.IMAGE_LIMIT
        // 업로드 단계에서 갈리는 셋.
        "IMAGE_TOO_LARGE", "IMAGE_INVALID_TYPE", "IMAGE_CORRUPTED" -> InquiryFailure.IMAGE_REJECTED
        "INQUIRY_NOT_FOUND" -> InquiryFailure.NOT_FOUND
        else -> InquiryFailure.UNKNOWN
    }
