package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.entity.user.InterestCategory
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 탐색: 실시간 인기 (GET /challenges/trending) ----------
@Serializable
data class TrendingChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
)

internal fun TrendingChallengeResponse.toDomain(): TrendingChallenge =
    TrendingChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        participantCount = participantCount ?: 0,
    )

@Serializable
data class TrendingChallengesResponse(
    @SerialName("challenges")
    val challenges: List<TrendingChallengeResponse>? = null,
)

internal fun TrendingChallengesResponse.toDomain(): List<TrendingChallenge> = challenges.orEmpty().map { it.toDomain() }

// ---------- 탐색: 카테고리별 챌린지 수 (GET /challenge-categories) ----------
@Serializable
data class ChallengeCategoryCountResponse(
    @SerialName("category")
    val category: String? = null,
    @SerialName("activeChallengeCount")
    val activeChallengeCount: Int? = null,
)

// 서버가 앱이 모르는 카테고리를 내려도 화면이 깨지지 않도록 매핑 실패 항목은 건너뛴다.
internal fun ChallengeCategoryCountResponse.toDomainOrNull(): ChallengeCategoryCount? =
    InterestCategory.fromValue(category.orEmpty())?.let { interestCategory ->
        ChallengeCategoryCount(
            category = interestCategory,
            activeChallengeCount = activeChallengeCount ?: 0,
        )
    }

@Serializable
data class ChallengeCategoriesResponse(
    @SerialName("categories")
    val categories: List<ChallengeCategoryCountResponse>? = null,
)

internal fun ChallengeCategoriesResponse.toDomain(): List<ChallengeCategoryCount> = categories.orEmpty().mapNotNull { it.toDomainOrNull() }

// ---------- 탐색: 챌린지 둘러보기 (GET /challenges/explore) ----------
@Serializable
data class ExploreChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("verificationMethod")
    val verificationMethod: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    // 표본 부족(누적 완료 참여자 10명 이하)이면 null
    @SerialName("completionRate")
    val completionRate: Double? = null,
    // 표본 부족(참여자 10명 이하 또는 진행률 30% 미만)이면 null
    @SerialName("successRate")
    val successRate: Double? = null,
    @SerialName("templateUsageCount")
    val templateUsageCount: Int? = null,
    @SerialName("endDate")
    val endDate: String? = null,
)

internal fun ExploreChallengeResponse.toDomain(): ExploreChallenge =
    ExploreChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        category = InterestCategory.fromValue(category.orEmpty()),
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        verificationMethod = SelectedMethod.fromValue(verificationMethod) ?: SelectedMethod.MANUAL,
        participantCount = participantCount ?: 0,
        completionRate = completionRate,
        successRate = successRate,
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
)

internal fun ExploreChallengesResponse.toDomain(): ExploreResult =
    ExploreResult(
        totalCount = totalCount ?: 0,
        challenges = challenges.orEmpty().map { it.toDomain() },
        nextCursor = nextCursor,
    )
