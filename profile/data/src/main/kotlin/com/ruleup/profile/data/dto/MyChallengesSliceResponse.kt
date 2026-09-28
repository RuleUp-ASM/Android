package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 내 챌린지 목록 (GET /challenges)
@Serializable
data class MyChallengeSliceResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    // GROUP / SOLO.
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
)

@Serializable
data class MyChallengesSliceResponse(
    @SerialName("challenges")
    val challenges: List<MyChallengeSliceResponse>? = null,
)

internal fun MyChallengesSliceResponse.toGroupChallenges(): List<GroupChallengeSummary> =
    challenges
        .orEmpty()
        .filter { (it.mode ?: it.participationType) == "GROUP" }
        .mapNotNull { item ->
            val id = item.challengeId ?: return@mapNotNull null
            GroupChallengeSummary(challengeId = id, title = item.title.orEmpty())
        }
