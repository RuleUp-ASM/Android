package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 내 챌린지 목록 (GET /challenges) — 그룹 랭킹 진입용 부분 역직렬화 ----------
@Serializable
data class MyChallengeSliceResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    // GROUP / SOLO
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
        .filter { it.participationType == "GROUP" }
        .mapNotNull { item ->
            val id = item.challengeId ?: return@mapNotNull null
            GroupChallengeSummary(challengeId = id, title = item.title.orEmpty())
        }
