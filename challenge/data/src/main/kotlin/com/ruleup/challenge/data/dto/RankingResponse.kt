package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.RankingEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 랭킹 (GET /challenges/{id}/ranking) · 방 홈 topRanking ----------
@Serializable
data class RankingEntryResponse(
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("userId")
    val userId: String? = null,
    // visibleNicknameTo + 익명 챌린지 마스킹 적용
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
    // 랭킹 API 전용 (방 홈 topRanking 에는 없음)
    @SerialName("successDays")
    val successDays: Int? = null,
)

internal fun RankingEntryResponse.toDomain(): RankingEntry =
    RankingEntry(
        rank = rank ?: 0,
        userId = userId.orEmpty(),
        nickname = nickname.orEmpty(),
        progressRate = progressRate ?: 0.0,
        successDays = successDays,
    )

@Serializable
data class MyRankResponse(
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
    // 1위면 null
    @SerialName("gapToAbove")
    val gapToAbove: Double? = null,
)

internal fun MyRankResponse.toDomain(): MyRank =
    MyRank(
        rank = rank ?: 0,
        progressRate = progressRate ?: 0.0,
        gapToAbove = gapToAbove,
    )

@Serializable
data class RankingResponse(
    @SerialName("rankings")
    val rankings: List<RankingEntryResponse>? = null,
    @SerialName("myRank")
    val myRank: MyRankResponse? = null,
)

internal fun RankingResponse.toDomain(): ChallengeRanking =
    ChallengeRanking(
        rankings = rankings.orEmpty().map { it.toDomain() },
        myRank = (myRank ?: MyRankResponse()).toDomain(),
    )
