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

// ---------- 접수 ----------

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
        // 접수번호가 없으면 사용자도 CS 도 이 문의를 특정할 수 없다 — 성공으로 그리면 안 된다.
        inquiryId = inquiryId.requireField("inquiryId"),
        status = InquiryStatus.fromValue(status),
        createdAt = createdAt.orEmpty(),
    )

// ---------- 목록 ----------

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

/**
 * id 가 비면 터뜨린다 — 그 행만 조용히 빼면 사용자는 접수한 문의가 목록에서 사라진 것을 보고
 * 다시 접수하고, 하루 상한만 깎인다. 목록을 못 그리는 편이 눈에 띄고 고칠 수 있다.
 */
internal fun InquirySummaryResponse.toDomain(): InquirySummary =
    InquirySummary(
        inquiryId = inquiryId.requireField("inquiries.items[].inquiryId"),
        category = InquiryCategory.fromValue(category),
        status = InquiryStatus.fromValue(status),
        preview = preview.orEmpty(),
        createdAt = createdAt.orEmpty(),
        answeredAt = answeredAt,
    )

// ---------- 상세 ----------

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

// ---------- 사진 업로드 ----------

@Serializable
data class InquiryImageResponse(
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

// ---------- 에러 코드 → 화면 어휘 ----------

/**
 * 서버 에러 코드를 화면이 아는 실패로 옮긴다. 여기 없는 코드는 [InquiryFailure.UNKNOWN] 이고,
 * 화면은 서버가 준 문구를 그대로 쓴다 — 서버가 코드를 추가해도 앱이 멈추지 않는다.
 *
 * `ACCOUNT_LOCKED` 를 다루지 않는 건 의도다 — 문의는 잠금 화이트리스트라 이 코드가 오지 않는다.
 * 온다면 서버 쪽 회귀이고, UNKNOWN 으로 떨어져 서버 문구가 그대로 보인다.
 */
internal fun ApiException.toInquiryFailure(): InquiryFailure =
    when (code) {
        "INQUIRY_DAILY_LIMIT" -> InquiryFailure.DAILY_LIMIT
        "INQUIRY_BODY_LENGTH" -> InquiryFailure.BODY_LENGTH
        "INQUIRY_IMAGE_LIMIT" -> InquiryFailure.IMAGE_LIMIT
        // 업로드 단계에서 갈리는 셋. 화면이 할 일은 "다른 사진을 고르세요"로 같다.
        "IMAGE_TOO_LARGE", "IMAGE_INVALID_TYPE", "IMAGE_CORRUPTED" -> InquiryFailure.IMAGE_REJECTED
        "INQUIRY_NOT_FOUND" -> InquiryFailure.NOT_FOUND
        else -> InquiryFailure.UNKNOWN
    }
