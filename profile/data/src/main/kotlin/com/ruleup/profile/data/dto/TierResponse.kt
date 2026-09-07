package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangeReason
import com.ruleup.profile.domain.entity.TierBest
import com.ruleup.profile.domain.entity.TierDemotion
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.entity.TierPromotion
import com.ruleup.profile.domain.entity.TierSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 내 티어 상세 (GET /me/tier) ----------
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
    // 시작점 −21. 이 점수 이하면 강등 확정
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
        // 표시 티어가 비면 실제 티어로 떨어뜨린다 — 없는 유예를 있는 것처럼 그리지 않는다.
        displayTier = displayTier?.let(Tier::fromValue) ?: Tier.fromValue(tier),
        graceBand = graceBand ?: false,
        promotion = promotion?.toDomain(),
        demotion = demotion?.toDomain(),
        // 날짜가 없는 행은 버린다 — 언제 일어난 변동인지 모르면 목록에 세울 자리가 없다.
        recentChanges = recentChanges.orEmpty().mapNotNull { it.toDomain() },
    )

/** 최상위 티어면 서버가 `promotion: null` 을 준다. 다음 티어 값이 비어도 같은 뜻으로 본다. */
internal fun TierPromotionResponse.toDomain(): TierPromotion? {
    val next = nextTier ?: return null
    return TierPromotion(
        nextTier = Tier.fromValue(next),
        // 남은 점수가 비면 0 — "곧 승급"이 아니라 표기만 생략된다.
        pointsToPromote = (pointsToPromote ?: 0).coerceAtLeast(0),
    )
}

/** 브론즈면 서버가 `demotion: null` 을 준다. 경계 둘 중 하나라도 비면 강등 안내를 그리지 않는다. */
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
        delta = delta ?: 0,
    )
}

// ---------- 티어 히스토리 (GET /me/tier/history) ----------
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
data class TierSnapshotResponse(
    // YYYY-MM
    @SerialName("month")
    val month: String? = null,
    @SerialName("endTier")
    val endTier: String? = null,
    @SerialName("endScore")
    val endScore: Int? = null,
)

@Serializable
data class TierHistoryResponse(
    @SerialName("best")
    val best: TierBestResponse? = null,
    @SerialName("monthly")
    val monthly: List<TierSnapshotResponse>? = null,
    @SerialName("retentionNote")
    val retentionNote: String? = null,
)

internal fun TierHistoryResponse.toDomain(): TierHistory =
    TierHistory(
        best = best?.toDomain(),
        // 월이 없는 스냅샷은 x축에 세울 자리가 없다.
        monthly = monthly.orEmpty().mapNotNull { it.toDomain() },
        retentionNote = retentionNote,
    )

/** 가입 직후처럼 표본이 없으면 서버가 `best` 를 비운다. 날짜가 없으면 최고 기록으로 세우지 않는다. */
internal fun TierBestResponse.toDomain(): TierBest? {
    val date = date ?: return null
    return TierBest(tier = Tier.fromValue(tier), score = score ?: 0, date = date)
}

internal fun TierSnapshotResponse.toDomain(): TierSnapshot? {
    val month = month ?: return null
    return TierSnapshot(month = month, endTier = Tier.fromValue(endTier), endScore = endScore ?: 0)
}
