package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 탐색: 실시간 인기 (GET /challenges/trending) ----------
@Serializable
data class TrendingChallengeResponse(
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("recentJoins24h")
    val recentJoins24h: Int? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("joinable")
    val joinable: Boolean? = null,
    @SerialName("endDate")
    val endDate: String? = null,
)

internal fun TrendingChallengeResponse.toDomain(index: Int): TrendingChallenge =
    TrendingChallenge(
        // rank 가 비면 배열 순서로 보정한다(1부터) — 서버가 이미 정렬해 내려준다.
        rank = rank ?: (index + 1),
        challengeId = challengeId.requireField("challengeId"),
        title = title.orEmpty(),
        imageUrl = imageUrl,
        category = Category.fromValue(category.orEmpty()),
        participantCount = participantCount ?: 0,
        recentJoins24h = recentJoins24h ?: 0,
        verificationType = VerificationType.fromValue(verificationType) ?: VerificationType.MANUAL,
        minTier = minTier?.let(Tier::fromValue),
        // 모르면 잠긴 것으로 본다 — 못 들어갈 방을 열려 있는 것처럼 보이게 하면 안 된다.
        joinable = joinable ?: false,
        endDate = endDate,
    )

@Serializable
data class TrendingChallengesResponse(
    // 순위 계산 기준 시각(ISO-8601, 최대 10분 지연)
    @SerialName("calculatedAt")
    val calculatedAt: String? = null,
    @SerialName("items")
    val items: List<TrendingChallengeResponse>? = null,
)

internal fun TrendingChallengesResponse.toDomain(): TrendingSnapshot =
    TrendingSnapshot(
        calculatedAt = calculatedAt,
        items = items.orEmpty().mapIndexed { index, item -> item.toDomain(index) },
    )

// ---------- 탐색: 카테고리별 챌린지 수 (GET /challenge-categories) ----------
@Serializable
data class ChallengeCategoryCountResponse(
    // 12종 enum code
    @SerialName("code")
    val code: String? = null,
    // 표시명(예: "운동")
    @SerialName("name")
    val name: String? = null,
    @SerialName("activeGroupCount")
    val activeGroupCount: Int? = null,
)

internal fun ChallengeCategoryCountResponse.toDomain(): ChallengeCategoryCount {
    val displayName = name.requireField("name")
    return ChallengeCategoryCount(
        name = displayName,
        activeGroupCount = activeGroupCount ?: 0,
        // code 로 매칭한다. 서버 코드와 앱 enum 이 어긋나던 시기의 표시명 폴백을 남겨 둔다.
        category = code?.let(Category::fromValue) ?: Category.entries.find { it.label == displayName },
    )
}

@Serializable
data class ChallengeCategoriesResponse(
    @SerialName("items")
    val items: List<ChallengeCategoryCountResponse>? = null,
)

internal fun ChallengeCategoriesResponse.toDomain(): List<ChallengeCategoryCount> = items.orEmpty().map { it.toDomain() }

// ---------- 탐색: 둘러보기 (GET /challenges/explore) ----------
@Serializable
data class ExploreChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("startsSoon")
    val startsSoon: Boolean? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("isFull")
    val isFull: Boolean? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("eligible")
    val eligible: Boolean? = null,
    @SerialName("completionRate")
    val completionRate: Double? = null,
    @SerialName("retentionRate")
    val retentionRate: Double? = null,
    @SerialName("dday")
    val dday: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

/**
 * **완주율·유지율의 null 은 기본값으로 접지 않는다** — 표본 미달을 뜻하는 값이라 0으로 바꾸면
 * "0%인 방"이라는 거짓 정보가 된다. 화면은 null 일 때 해당 영역을 숨긴다.
 */
internal fun ExploreChallengeResponse.toDomain(): ExploreChallenge =
    ExploreChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.orEmpty(),
        imageUrl = imageUrl,
        category = Category.fromValue(category.orEmpty()),
        verificationType = VerificationType.fromValue(verificationType) ?: VerificationType.MANUAL,
        startsSoon = startsSoon ?: false,
        participantCount = participantCount ?: 0,
        capacity = capacity ?: 0,
        isFull = isFull ?: false,
        minTier = minTier?.let(Tier::fromValue),
        // 모르면 막는 쪽으로 — 못 들어갈 방에 참여 동선을 열어주지 않는다.
        eligible = eligible ?: false,
        completionRate = completionRate,
        retentionRate = retentionRate,
        dday = dday,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
    )

@Serializable
data class ExploreChallengesResponse(
    @SerialName("items")
    val items: List<ExploreChallengeResponse>? = null,
    // 마지막 페이지면 null
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("hasNext")
    val hasNext: Boolean? = null,
)

internal fun ExploreChallengesResponse.toDomain(): ExploreResult {
    val more = hasNext ?: (nextCursor != null)
    return ExploreResult(
        items = items.orEmpty().map { it.toDomain() },
        // hasNext=false 인데 커서가 남아 있는 비정상 응답에서 무한 요청이 돌지 않게 막는다.
        nextCursor = nextCursor.takeIf { more },
        hasNext = more,
    )
}
