package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.ObjectionDecisionResult
import com.ruleup.verification.domain.entity.ObjectionStatus
import com.ruleup.verification.domain.entity.ObjectionTicket
import com.ruleup.verification.domain.entity.ObjectionType
import com.ruleup.verification.domain.entity.PendingReviewItem
import com.ruleup.verification.domain.entity.PendingReviews
import com.ruleup.verification.domain.entity.ReviewKind
import com.ruleup.verification.domain.entity.VerifiedVia
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 이의 제기 제출 (POST objections) ----------
@Serializable
data class SubmitObjectionRequest(
    @SerialName("type")
    val type: String,
    @SerialName("targetDate")
    val targetDate: String,
    @SerialName("content")
    val content: String,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

@Serializable
data class ObjectionResponse(
    @SerialName("objectionId")
    val objectionId: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("deadline")
    val deadline: String? = null,
)

internal fun ObjectionResponse.toDomain(): ObjectionTicket =
    ObjectionTicket(
        objectionId = objectionId.orEmpty(),
        type = ObjectionType.fromValue(type) ?: ObjectionType.FAILURE,
        status = ObjectionStatus.fromValue(status) ?: ObjectionStatus.PENDING,
        deadline = deadline.orEmpty(),
    )

// ---------- 이의 제기 승인/기각 (POST objections/{id}/decision) ----------
@Serializable
data class ObjectionDecisionRequest(
    @SerialName("decision")
    val decision: String,
    @SerialName("reason")
    val reason: String? = null,
)

@Serializable
data class ObjectionDecisionResponse(
    @SerialName("objectionId")
    val objectionId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("resultStatus")
    val resultStatus: String? = null,
    @SerialName("verifiedVia")
    val verifiedVia: String? = null,
)

internal fun ObjectionDecisionResponse.toDomain(): ObjectionDecisionResult =
    ObjectionDecisionResult(
        objectionId = objectionId.orEmpty(),
        status = ObjectionStatus.fromValue(status) ?: ObjectionStatus.PENDING,
        targetDate = targetDate.orEmpty(),
        resultStatus = resultStatus.orEmpty(),
        verifiedVia = VerifiedVia.fromValue(verifiedVia),
    )

// ---------- 확인 대기함 (GET pending-reviews) ----------
@Serializable
data class PendingReviewItemResponse(
    @SerialName("kind")
    val kind: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("targetDate")
    val targetDate: String? = null,
    @SerialName("content")
    val content: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("submittedAt")
    val submittedAt: String? = null,
    @SerialName("deadline")
    val deadline: String? = null,
)

@Serializable
data class PendingReviewsResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("pendingCount")
    val pendingCount: Int? = null,
    @SerialName("items")
    val items: List<PendingReviewItemResponse>? = null,
)

internal fun PendingReviewsResponse.toDomain(): PendingReviews =
    PendingReviews(
        challengeId = challengeId.orEmpty(),
        items =
            items.orEmpty().map { item ->
                PendingReviewItem(
                    kind = ReviewKind.fromValue(item.kind) ?: ReviewKind.FALLBACK,
                    id = item.id.orEmpty(),
                    userId = item.userId.orEmpty(),
                    nickname = item.nickname.orEmpty(),
                    targetDate = item.targetDate.orEmpty(),
                    content = item.content,
                    imageUrl = item.imageUrl,
                    submittedAt = item.submittedAt.orEmpty(),
                    deadline = item.deadline,
                )
            },
    )
