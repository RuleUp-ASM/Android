package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.domain.entity.category.Category
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
    @SerialName("category")
    val category: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("recentJoins24h")
    val recentJoins24h: Int? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
    @SerialName("endDate")
    val endDate: String? = null,
)

internal fun TrendingChallengeResponse.toDomain(index: Int): TrendingChallenge =
    TrendingChallenge(
        // rank 누락 시 배열 순서로 보정(1부터)
        rank = rank ?: (index + 1),
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        participantCount = participantCount ?: 0,
    )

@Serializable
data class TrendingChallengesResponse(
    // 랭킹 계산 기준 시각(ISO-8601, 최대 10분 지연)
    @SerialName("calculatedAt")
    val calculatedAt: String? = null,
    @SerialName("items")
    val items: List<TrendingChallengeResponse>? = null,
)

internal fun TrendingChallengesResponse.toDomain(): List<TrendingChallenge> =
    items.orEmpty().mapIndexed { index, item -> item.toDomain(index) }

// ---------- 탐색: 카테고리별 챌린지 수 (GET /challenge-categories) ----------
@Serializable
data class ChallengeCategoryCountResponse(
    @SerialName("categoryId")
    val categoryId: Long? = null,
    // 표시명(예: "운동")
    @SerialName("name")
    val name: String? = null,
    @SerialName("activeChallengeCount")
    val activeChallengeCount: Int? = null,
)

internal fun ChallengeCategoryCountResponse.toDomain(): ChallengeCategoryCount {
    val displayName = name.requireField("name")
    return ChallengeCategoryCount(
        categoryId = categoryId.requireField("categoryId"),
        name = displayName,
        activeChallengeCount = activeChallengeCount ?: 0,
        // 서버는 표시명만 주므로 앱 카테고리(아이콘·필터)는 라벨로 매칭한다. 실패 시 null(기본 아이콘).
        category = Category.entries.find { it.label == displayName },
    )
}

@Serializable
data class ChallengeCategoriesResponse(
    @SerialName("items")
    val items: List<ChallengeCategoryCountResponse>? = null,
)

internal fun ChallengeCategoriesResponse.toDomain(): List<ChallengeCategoryCount> = items.orEmpty().map { it.toDomain() }

// ---------- 탐색: 챌린지 둘러보기 (GET /challenges/explore) ----------
@Serializable
data class SuccessFailRatioResponse(
    @SerialName("successCount")
    val successCount: Int? = null,
    @SerialName("failCount")
    val failCount: Int? = null,
    // 성공률 0~1 (정렬 값)
    @SerialName("successRate")
    val successRate: Double? = null,
)

@Serializable
data class ExploreChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("templateId")
    val templateId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("anonymity")
    val anonymity: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
    // minMannerTemperature ≤ 내 매너 온도 계산 결과(잠금 표시용, 항상 반환)
    @SerialName("joinable")
    val joinable: Boolean? = null,
    @SerialName("templateUsageCount")
    val templateUsageCount: Int? = null,
    // 완주율(템플릿 단위, 0~1). 누적 완료 참여자 ≤ 10이면 null
    @SerialName("completionRate")
    val completionRate: Double? = null,
    // 성공/실패(방 단위). 참여자 ≤ 10 또는 진행률 < 30%면 null
    @SerialName("successFailRatio")
    val successFailRatio: SuccessFailRatioResponse? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

internal fun ExploreChallengeResponse.toDomain(): ExploreChallenge =
    ExploreChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        category = Category.fromValue(category.orEmpty()),
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        verificationMethod = SelectedMethod.fromValue(verificationType) ?: SelectedMethod.MANUAL,
        participantCount = participantCount ?: 0,
        completionRate = completionRate,
        successRate = successFailRatio?.successRate,
        templateUsageCount = templateUsageCount ?: 0,
        endDate = endDate,
    )

@Serializable
data class ExploreChallengesResponse(
    @SerialName("totalCount")
    val totalCount: Int? = null,
    @SerialName("challenges")
    val challenges: List<ExploreChallengeResponse>? = null,
    // 마지막 페이지면 null
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("hasNext")
    val hasNext: Boolean? = null,
)

internal fun ExploreChallengesResponse.toDomain(): ExploreResult =
    ExploreResult(
        totalCount = totalCount ?: 0,
        challenges = challenges.orEmpty().map { it.toDomain() },
        // hasNext=false 인데 커서가 남아있는 비정상 응답도 방어한다.
        nextCursor = if (hasNext == false) null else nextCursor,
    )
