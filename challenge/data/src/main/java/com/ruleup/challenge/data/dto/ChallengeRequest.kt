package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.1 LLM 기본값 추천 ----------
@Serializable
data class RecommendationRequest(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
)

// ---------- 3.2 챌린지 생성 ----------
@Serializable
data class CreateChallengeRequest(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    // endDate 는 서버가 파생하므로 전송하지 않는다
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("verificationMethods")
    val verificationMethods: List<String>? = null,
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
    @SerialName("anonymity")
    val anonymity: String? = null,
)

internal fun ChallengeForm.toRequest(): CreateChallengeRequest =
    CreateChallengeRequest(
        title = title,
        description = description,
        imageUrl = imageUrl,
        category = category.value,
        participationType = participationType.value,
        minMannerTemperature = minMannerTemperature,
        repeatDays = repeatDays.map { it.value },
        durationDays = durationDays,
        startDate = startDate,
        verificationMethods = verificationMethods.map { it.value },
        penalty = penalty.toDto(),
        reward = reward.toDto(),
        anonymity = anonymity.value,
    )

// ---------- 3.4 챌린지 수정 (변경할 필드만 전달) ----------
@Serializable
data class UpdateChallengeRequest(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("verificationMethods")
    val verificationMethods: List<String>? = null,
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
)

internal fun ChallengeUpdate.toRequest(): UpdateChallengeRequest =
    UpdateChallengeRequest(
        title = title,
        description = description,
        category = category?.value,
        repeatDays = repeatDays?.map { it.value },
        durationDays = durationDays,
        startDate = startDate,
        verificationMethods = verificationMethods?.map { it.value },
        penalty = penalty?.toDto(),
        reward = reward?.toDto(),
        minMannerTemperature = minMannerTemperature,
    )

// ---------- 3.7 참여 승인/거절 ----------
@Serializable
data class MemberDecisionRequest(
    @SerialName("action")
    val action: String? = null,
)
