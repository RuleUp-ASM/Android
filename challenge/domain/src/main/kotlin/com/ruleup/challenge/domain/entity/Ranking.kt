package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.user.User

/** 랭킹 등재 기준. */
object RankingPolicy {
    // 방 안 랭킹: 참여 10회 이상
    const val IN_ROOM_MIN_PARTICIPATIONS = 10

    // 방 밖 랭킹: 그룹 누적 50회 / 솔로 누적 10회 이상
    const val CROSS_GROUP_MIN_TOTAL = 50
    const val CROSS_SOLO_MIN_TOTAL = 10
}

/** 방 안 랭킹 항목. */
data class RankingEntry(
    // 10회 미만 참여자는 미등재
    val rank: Int?,
    val user: User,
    // 성공률 0~1.
    val successRate: Double?,
    val successCount: Int,
    val participations: Int,
)

/** 내 방 안 순위. */
data class MyRank(
    val rank: Int?,
    val ranked: Boolean,
    val successRate: Double?,
    val participations: Int,
    // 1위와의 성공률 차 (1위면 0.0)
    val gapToFirst: Double?,
)

/** 방 안 랭킹. */
data class ChallengeRanking(
    val me: MyRank,
    val items: List<RankingEntry>,
)

/** 방 밖 랭킹 비교 모드. */
enum class RankingMode(
    val value: String,
) {
    GROUP("GROUP"),
    SOLO("SOLO"),
}

/** 방 밖 랭킹 항목 */
data class ChallengeRankEntry(
    val rank: Int,
    val challengeId: String,
    val title: String,
    val memberCount: Int,
    // 누적 진행 횟수
    val totalCount: Int,
    // 방 성공률 0~1
    val successRate: Double,
)

/** 내 방의 방 밖 순위. */
data class MyChallengeRank(
    val challengeId: String,
    val rank: Int?,
    val ranked: Boolean,
    val successRate: Double?,
    val totalCount: Int,
)

/** 방 밖 랭킹. */
data class CrossChallengeRanking(
    val myChallenge: MyChallengeRank?,
    val items: List<ChallengeRankEntry>,
    // ISO datetime
    val updatedAt: String?,
    val nextCursor: String?,
)
