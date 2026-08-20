package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeRankEntry
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.MyChallengeRank
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.RankingEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 방 안 랭킹 (GET /challenges/{id}/ranking) ----------

@Serializable
data class RankingEntryResponse(
    // 10회 미만 참여자는 미등재 — null 로 내려온다
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("user")
    val user: RoomUserResponse? = null,
    @SerialName("successRate")
    val successRate: Double? = null,
    @SerialName("successCount")
    val successCount: Int? = null,
    @SerialName("participations")
    val participations: Int? = null,
)

// rank·successRate 의 null 은 "미등재"라는 사실이라 기본값으로 접지 않는다.
internal fun RankingEntryResponse.toDomain(): RankingEntry =
    RankingEntry(
        rank = rank,
        user = user.toDomain(),
        successRate = successRate,
        successCount = successCount ?: 0,
        participations = participations ?: 0,
    )

@Serializable
data class MyRankResponse(
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("ranked")
    val ranked: Boolean? = null,
    @SerialName("successRate")
    val successRate: Double? = null,
    @SerialName("participations")
    val participations: Int? = null,
    // 1위와의 성공률 차 (1위면 0.0)
    @SerialName("gapToFirst")
    val gapToFirst: Double? = null,
)

internal fun MyRankResponse.toDomain(): MyRank =
    MyRank(
        rank = rank,
        // ranked 가 없으면 rank 유무로 판정한다 — 둘은 같은 사실의 다른 표현이다
        ranked = ranked ?: (rank != null),
        successRate = successRate,
        participations = participations ?: 0,
        gapToFirst = gapToFirst,
    )

@Serializable
data class RankingResponse(
    @SerialName("me")
    val me: MyRankResponse? = null,
    @SerialName("items")
    val items: List<RankingEntryResponse>? = null,
)

internal fun RankingResponse.toDomain(): ChallengeRanking =
    ChallengeRanking(
        me = (me ?: MyRankResponse()).toDomain(),
        items = items.orEmpty().map { it.toDomain() },
    )

// ---------- 방 밖 랭킹 (GET /rankings/challenges) ----------

@Serializable
data class ChallengeRankEntryResponse(
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("memberCount")
    val memberCount: Int? = null,
    @SerialName("totalCount")
    val totalCount: Int? = null,
    @SerialName("successRate")
    val successRate: Double? = null,
)

/**
 * 목록에는 **등재된 방만** 내려온다(그룹 50회·솔로 10회 이상) — 그래서 [rank]·[successRate] 는
 * 항상 있다. 없으면 미등재가 아니라 깨진 행이므로 0 등·0% 로 채우지 않고 통째로 버린다.
 * 랭킹 화면에 "0위 · 0%" 가 섞이면 사용자는 그 방이 꼴찌라고 읽는다.
 */
internal fun ChallengeRankEntryResponse.toDomain(): ChallengeRankEntry? {
    val rank = rank ?: return null
    val successRate = successRate ?: return null
    val challengeId = challengeId ?: return null
    return ChallengeRankEntry(
        rank = rank,
        challengeId = challengeId,
        title = title.orEmpty(),
        memberCount = memberCount ?: 0,
        totalCount = totalCount ?: 0,
        successRate = successRate,
    )
}

@Serializable
data class MyChallengeRankResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("ranked")
    val ranked: Boolean? = null,
    @SerialName("successRate")
    val successRate: Double? = null,
    @SerialName("totalCount")
    val totalCount: Int? = null,
)

internal fun MyChallengeRankResponse.toDomain(): MyChallengeRank =
    MyChallengeRank(
        challengeId = challengeId.orEmpty(),
        rank = rank,
        ranked = ranked ?: (rank != null),
        successRate = successRate,
        totalCount = totalCount ?: 0,
    )

@Serializable
data class CrossRankingResponse(
    // 요청에 challengeId 를 주지 않았으면 null
    @SerialName("myChallenge")
    val myChallenge: MyChallengeRankResponse? = null,
    @SerialName("items")
    val items: List<ChallengeRankEntryResponse>? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("nextCursor")
    val nextCursor: String? = null,
)

internal fun CrossRankingResponse.toDomain(): CrossChallengeRanking =
    CrossChallengeRanking(
        myChallenge = myChallenge?.toDomain(),
        items = items.orEmpty().mapNotNull { it.toDomain() },
        updatedAt = updatedAt,
        nextCursor = nextCursor,
    )
