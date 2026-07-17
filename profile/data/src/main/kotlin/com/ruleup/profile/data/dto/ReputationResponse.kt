package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.MilestoneType
import com.ruleup.profile.domain.entity.NextTier
import com.ruleup.profile.domain.entity.ReputationChange
import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.profile.domain.entity.ReputationHistory
import com.ruleup.profile.domain.entity.ReputationMilestone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 매너 온도 상세 (GET /me/reputation) ----------
@Serializable
data class NextTierResponse(
    @SerialName("target")
    val target: Double? = null,
    // 이전 앵커 → 다음 앵커 구간 진행률 (0~1)
    @SerialName("progressRate")
    val progressRate: Double? = null,
    @SerialName("label")
    val label: String? = null,
)

@Serializable
data class ReputationChangeResponse(
    @SerialName("date")
    val date: String? = null,
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("delta")
    val delta: Double? = null,
    @SerialName("label")
    val label: String? = null,
)

@Serializable
data class ReputationResponse(
    @SerialName("current")
    val current: Double? = null,
    @SerialName("bandLabel")
    val bandLabel: String? = null,
    @SerialName("nextTier")
    val nextTier: NextTierResponse? = null,
    @SerialName("recentChanges")
    val recentChanges: List<ReputationChangeResponse>? = null,
)

internal fun ReputationResponse.toDomain(): ReputationDetail =
    ReputationDetail(
        current = current ?: 0.0,
        bandLabel = bandLabel.orEmpty(),
        nextTier =
            nextTier?.target?.let { target ->
                NextTier(
                    target = target,
                    progressRate = (nextTier.progressRate ?: 0.0).coerceIn(0.0, 1.0),
                    label = nextTier.label,
                )
            },
        recentChanges =
            recentChanges.orEmpty().map {
                ReputationChange(
                    date = it.date.orEmpty(),
                    temperature = it.temperature ?: 0.0,
                    delta = it.delta ?: 0.0,
                    label = it.label.orEmpty(),
                )
            },
    )

// ---------- 평판 히스토리 (GET /me/reputation/history) ----------
@Serializable
data class ReputationPeakResponse(
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("achievedAt")
    val achievedAt: String? = null,
)

@Serializable
data class ReputationMilestoneResponse(
    // TIER_REACHED / STREAK / FIRST_COMPLETION / SIGNUP
    @SerialName("type")
    val type: String? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("achievedAt")
    val achievedAt: String? = null,
)

@Serializable
data class ReputationHistoryResponse(
    @SerialName("peak")
    val peak: ReputationPeakResponse? = null,
    @SerialName("milestones")
    val milestones: List<ReputationMilestoneResponse>? = null,
)

internal fun ReputationHistoryResponse.toDomain(): ReputationHistory =
    ReputationHistory(
        peakTemperature = peak?.temperature ?: 0.0,
        peakAchievedAt = peak?.achievedAt.orEmpty(),
        milestones =
            milestones.orEmpty().map {
                ReputationMilestone(
                    type = MilestoneType.fromValue(it.type),
                    label = it.label.orEmpty(),
                    achievedAt = it.achievedAt.orEmpty(),
                )
            },
    )
