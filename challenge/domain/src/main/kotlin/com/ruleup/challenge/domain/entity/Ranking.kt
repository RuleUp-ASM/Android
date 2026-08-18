package com.ruleup.challenge.domain.entity

/** 랭킹 등재 기준 (명세). 미달자는 목록에는 남되 등수가 null 이라 화면이 "-" 를 그린다. */
object RankingPolicy {
    // 방 안 랭킹: 참여 10회 이상
    const val IN_ROOM_MIN_PARTICIPATIONS = 10

    // 방 밖 랭킹: 그룹 누적 50회 / 솔로 누적 10회 이상
    const val CROSS_GROUP_MIN_TOTAL = 50
    const val CROSS_SOLO_MIN_TOTAL = 10
}

/**
 * 방 안 랭킹 항목 (명세: GET /challenges/{id}/ranking `items[]`).
 *
 * 기준은 **참여한 날 이후의 전체 성공률** 하나뿐이다(주간/월간 탭 폐기). 동점이면 같은 [rank] 를
 * 부여하고 [successCount] 로 표시 순서를 가린다.
 */
data class RankingEntry(
    // 10회 미만 참여자는 미등재 — null
    val rank: Int?,
    val user: RoomUser,
    // 성공률 0~1. 미등재면 null 이며 0 으로 접지 않는다
    val successRate: Double?,
    val successCount: Int,
    val participations: Int,
)

/**
 * 내 방 안 순위 (명세 `me`). [ranked] 가 false 면 [rank]·[successRate] 가 null 이고
 * 화면은 "-" 와 함께 [participations] 를 근거로 "10회부터 등재" 를 안내한다.
 */
data class MyRank(
    val rank: Int?,
    val ranked: Boolean,
    val successRate: Double?,
    val participations: Int,
    // 1위와의 성공률 차 (1위면 0.0)
    val gapToFirst: Double?,
)

/** 방 안 랭킹 (명세: GET /challenges/{id}/ranking). 완료된 방도 최종 랭킹 열람용으로 조회된다. */
data class ChallengeRanking(
    val me: MyRank,
    val items: List<RankingEntry>,
)

/** 방 밖 랭킹 비교 모드 (명세 `mode`). 같은 모드끼리만 비교한다. */
enum class RankingMode(
    val value: String,
) {
    GROUP("GROUP"),
    SOLO("SOLO"),
}

/** 방 밖 랭킹 항목 (명세: GET /rankings/challenges `items[]`) — 비교 단위가 사람이 아니라 방이다. */
data class ChallengeRankEntry(
    val rank: Int,
    val challengeId: String,
    val title: String,
    val memberCount: Int,
    // 누적 진행 횟수 — 등재 기준값
    val totalCount: Int,
    // 방 성공률 0~1
    val successRate: Double,
)

/** 내 방의 방 밖 순위 (명세 `myChallenge`). 요청에 challengeId 를 주지 않았으면 응답 자체가 null 이다. */
data class MyChallengeRank(
    val challengeId: String,
    val rank: Int?,
    val ranked: Boolean,
    val successRate: Double?,
    val totalCount: Int,
)

/**
 * 방 밖 랭킹 (명세: GET /rankings/challenges). **하루 1회 03시 배치 스냅샷**이라 실시간이 아니다 —
 * [updatedAt] 을 화면에 명시해 방금 인증한 결과가 반영되지 않은 이유를 설명한다.
 */
data class CrossChallengeRanking(
    val myChallenge: MyChallengeRank?,
    val items: List<ChallengeRankEntry>,
    // ISO datetime
    val updatedAt: String?,
    val nextCursor: String?,
)
