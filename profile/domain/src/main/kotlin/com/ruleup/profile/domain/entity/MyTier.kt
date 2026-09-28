package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.Tier

/** 점수가 움직인 사유. */
enum class ScoreChangeReason(
    val value: String,
) {
    CYCLE_SUCCESS("CYCLE_SUCCESS"),
    CYCLE_FAIL("CYCLE_FAIL"),

    // 중도 탈퇴 −15 (해당 챌린지 1년 이상 성공 시 면제)
    LEAVE("LEAVE"),

    // 연속 실패 강퇴 −15
    KICK_FAIL("KICK_FAIL"),

    // 권한 미허용 강퇴 −15
    KICK_PERMISSION("KICK_PERMISSION"),

    // 부정행위 검출 −50.
    CHEAT("CHEAT"),

    // 이의 인용 소급
    APPEAL_RESTORE("APPEAL_RESTORE"),
    ;

    companion object {
        /** 미지 값은 null */
        fun fromValue(value: String?): ScoreChangeReason? = entries.find { it.value == value }
    }
}

/** 다음 티어까지. */
data class TierPromotion(
    val nextTier: Tier,
    // 다음 티어 시작점까지 남은 점수
    val pointsToPromote: Int,
)

/** 강등 경계. */
data class TierDemotion(
    // 표시 티어 시작점 −20 (유예 하한)
    val graceFloor: Int,
    // 시작점 −21.
    val demoteAt: Int,
)

/** 점수 변동 1건. */
data class ScoreChange(
    // YYYY-MM-DD (KST)
    val date: String,
    val reason: ScoreChangeReason?,
    // 변동을 일으킨 챌린지.
    val challengeId: String?,
    val challengeTitle: String?,
    val delta: Int,
)

/** 점수 변동 이력 한 페이지. */
data class ScoreChangePage(
    val items: List<ScoreChange>,
    val nextCursor: String?,
    // 보관 일수
    val retentionDays: Int?,
)

/** 내 티어 상세. */
data class MyTier(
    val tier: Tier,
    // 누적 점수 0~2,000
    val score: Int,
    val displayTier: Tier,
    val graceBand: Boolean,
    val promotion: TierPromotion?,
    val demotion: TierDemotion?,
    val recentChanges: List<ScoreChange>,
) {
    /** [displayTier] 구간 안에서의 진행률 0.0~1.0. */
    val progressInDisplayTier: Float
        get() {
            val span = displayTier.maxScore - displayTier.minScore
            if (span <= 0) return 1f
            return ((score - displayTier.minScore).toFloat() / span).coerceIn(0f, 1f)
        }

    /** 화면에 세울 승급 안내. */
    val displayPromotion: TierPromotion?
        get() = promotion?.takeIf { it.nextTier.ordinal > displayTier.ordinal }

    /** 유예 밴드에서 강등이 확정되는 점수. */
    val graceFloorScore: Int?
        get() = demotion?.graceFloor?.takeIf { graceBand }
}

/** 점수가 움직인 한 시점. */
data class TierPoint(
    // ISO-8601
    val occurredAt: String,
    val tier: Tier,
    val score: Int,
) {
    /** x축 라벨용 `YYYY-MM`. */
    val month: String
        get() = occurredAt.take(7)
}

/** 역대 최고. */
data class TierBest(
    val tier: Tier,
    val score: Int,
    // YYYY-MM-DD
    val date: String,
)

/** 티어 히스토리. */
data class TierHistory(
    val best: TierBest?,
    val points: List<TierPoint>,
    val retentionNote: String?,
)
