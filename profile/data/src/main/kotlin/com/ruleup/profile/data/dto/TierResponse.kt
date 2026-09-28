package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangePage
import com.ruleup.profile.domain.entity.ScoreChangeReason
import com.ruleup.profile.domain.entity.TierBest
import com.ruleup.profile.domain.entity.TierDemotion
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.entity.TierPoint
import com.ruleup.profile.domain.entity.TierPromotion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 내 티어 상세 (GET /me/tier)
@Serializable
data class TierPromotionResponse(
    @SerialName("nextTier")
    val nextTier: String? = null,
    @SerialName("pointsToPromote")
    val pointsToPromote: Int? = null,
)

@Serializable
data class TierDemotionResponse(
    // 표시 티어 시작점 −20 (유예 하한)
    @SerialName("graceFloor")
    val graceFloor: Int? = null,
    // 시작점 −21.
    @SerialName("demoteAt")
    val demoteAt: Int? = null,
)

@Serializable
data class ScoreChangeResponse(
    @SerialName("date")
    val date: String? = null,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("challengeTitle")
    val challengeTitle: String? = null,
    @SerialName("delta")
    val delta: Int? = null,
)

@Serializable
data class MyTierResponse(
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("score")
    val score: Int? = null,
    @SerialName("displayTier")
    val displayTier: String? = null,
    @SerialName("graceBand")
    val graceBand: Boolean? = null,
    @SerialName("promotion")
    val promotion: TierPromotionResponse? = null,
    @SerialName("demotion")
    val demotion: TierDemotionResponse? = null,
    @SerialName("recentChanges")
    val recentChanges: List<ScoreChangeResponse>? = null,
)

internal fun MyTierResponse.toDomain(): MyTier =
    MyTier(
        tier = Tier.fromValue(tier),
        score = score ?: 0,
        // 표시 티어가 비면 실제 티어로 떨어뜨린다
        displayTier = displayTier?.let(Tier::fromValue) ?: Tier.fromValue(tier),
        graceBand = graceBand ?: false,
        promotion = promotion?.toDomain(),
        demotion = demotion?.toDomain(),
        // 날짜가 없는 행은 버린다
        recentChanges = recentChanges.orEmpty().mapNotNull { it.toDomain() },
    )

/** 최상위 티어면 서버가 `promotion: null` 을 준다. */
internal fun TierPromotionResponse.toDomain(): TierPromotion? {
    val next = nextTier ?: return null
    return TierPromotion(
        nextTier = Tier.fromValue(next),
        // 남은 점수가 비면 0
        pointsToPromote = (pointsToPromote ?: 0).coerceAtLeast(0),
    )
}

/** 브론즈면 서버가 `demotion: null` 을 준다. */
internal fun TierDemotionResponse.toDomain(): TierDemotion? {
    val floor = graceFloor ?: return null
    val at = demoteAt ?: return null
    return TierDemotion(graceFloor = floor, demoteAt = at)
}

internal fun ScoreChangeResponse.toDomain(): ScoreChange? {
    val date = date ?: return null
    return ScoreChange(
        date = date,
        reason = ScoreChangeReason.fromValue(reason),
        challengeId = challengeId,
        // 완료 방은 원본이 하드 삭제돼 서버도 이름을 못 채운다.
        challengeTitle = challengeTitle?.takeIf { it.isNotBlank() },
        delta = delta ?: 0,
    )
}

// 점수 변동 이력 (GET /me/tier/changes)
@Serializable
data class ScoreChangesResponse(
    @SerialName("items")
    val items: List<ScoreChangeResponse>? = null,
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("retentionDays")
    val retentionDays: Int? = null,
)

internal fun ScoreChangesResponse.toDomain(): ScoreChangePage =
    ScoreChangePage(
        // 날짜 없는 행은 버린다
        items = items.orEmpty().mapNotNull { it.toDomain() },
        nextCursor = nextCursor?.takeIf { it.isNotBlank() },
        retentionDays = retentionDays,
    )

// 티어 히스토리 (GET /me/tier/history)
@Serializable
data class TierBestResponse(
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("score")
    val score: Int? = null,
    @SerialName("date")
    val date: String? = null,
)

@Serializable
data class TierPointResponse(
    // ISO-8601
    @SerialName("occurredAt")
    val occurredAt: String? = null,
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("score")
    val score: Int? = null,
)

@Serializable
data class TierHistoryResponse(
    @SerialName("best")
    val best: TierBestResponse? = null,
    @SerialName("points")
    val points: List<TierPointResponse>? = null,
    @SerialName("retentionNote")
    val retentionNote: String? = null,
)

internal fun TierHistoryResponse.toDomain(): TierHistory =
    TierHistory(
        best = best?.toDomain(),
        // 시각이 없는 점은 x축에 세울 자리가 없다.
        points = points.orEmpty().mapNotNull { it.toDomain() },
        retentionNote = retentionNote,
    )

/** 가입 직후처럼 표본이 없으면 서버가 `best` 를 비운다. */
internal fun TierBestResponse.toDomain(): TierBest? {
    val date = date ?: return null
    return TierBest(tier = Tier.fromValue(tier), score = score ?: 0, date = date)
}

internal fun TierPointResponse.toDomain(): TierPoint? {
    val occurredAt = occurredAt ?: return null
    return TierPoint(occurredAt = occurredAt, tier = Tier.fromValue(tier), score = score ?: 0)
}
