package com.ruleup.verification.data.dto

import com.ruleup.entity.user.InterestCategory
import com.ruleup.network.dto.requireField
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.TodayStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.2 진행률 일괄 조회 ----------
@Serializable
data class ChallengeProgressResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
    @SerialName("successDays")
    val successDays: Int? = null,
    @SerialName("targetDays")
    val targetDays: Int? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
    @SerialName("todayTarget")
    val todayTarget: Boolean? = null,
    @SerialName("todayStatus")
    val todayStatus: String? = null,
    @SerialName("lastSyncedAt")
    val lastSyncedAt: String? = null,
)

@Serializable
data class ProgressResponse(
    @SerialName("asOf")
    val asOf: String? = null,
    @SerialName("challenges")
    val challenges: List<ChallengeProgressResponse>? = null,
)

internal fun ChallengeProgressResponse.toDomain(): ChallengeProgress =
    ChallengeProgress(
        challengeId = challengeId.requireField("challengeId"),
        title = title.orEmpty(),
        category = InterestCategory.fromValue(category.orEmpty()),
        participationType = participationType,
        status = status.orEmpty(),
        progressRate = progressRate ?: 0.0,
        successDays = successDays ?: 0,
        targetDays = targetDays ?: 0,
        remainingDays = remainingDays ?: 0,
        todayTarget = todayTarget ?: false,
        todayStatus = TodayStatus.fromValue(todayStatus),
        lastSyncedAt = lastSyncedAt,
    )

internal fun ProgressResponse.toDomain(): ProgressSnapshot =
    ProgressSnapshot(
        asOf = asOf.orEmpty(),
        challenges = challenges.orEmpty().map { it.toDomain() },
    )
