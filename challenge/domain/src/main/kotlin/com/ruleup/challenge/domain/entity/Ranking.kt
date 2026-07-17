package com.ruleup.challenge.domain.entity

/**
 * 랭킹 항목 (명세 rankings[] · 방 홈 topRanking[]).
 * 정렬은 서버가 확정 배치 기준 비정규화 진행률로 수행한다 (progressRate desc → successDays desc → joinedAt asc).
 */
data class RankingEntry(
    val rank: Int,
    val userId: String,
    // visibleNicknameTo + 익명 챌린지 마스킹 적용된 표시명
    val nickname: String,
    // 완주율 (%)
    val progressRate: Double,
    // 랭킹 API 전용 — 방 홈 topRanking 에는 내려오지 않는다
    val successDays: Int? = null,
)

/** 내 순위 (명세 myRank). */
data class MyRank(
    val rank: Int,
    val progressRate: Double,
    // 바로 위 순위와의 완주율 격차 (1위면 null)
    val gapToAbove: Double?,
)

/** 그룹 랭킹 (명세: GET /challenges/{id}/ranking). ACTIVE 멤버 전용. */
data class ChallengeRanking(
    val rankings: List<RankingEntry>,
    val myRank: MyRank,
)
