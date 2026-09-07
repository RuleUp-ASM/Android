package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.Tier

/**
 * 점수가 움직인 사유 (명세 `recentChanges[].reason`).
 *
 * 신고 강퇴(`KICK_REPORT`)는 없다 — 신고 경로의 챌린지 강퇴가 폐지되어 발생하지 않는다.
 * 계정 제재로 전 챌린지에서 자동 탈퇴돼도 **감점 이벤트 자체가 생성되지 않는다**(제재 정책 §5.3·§7).
 */
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

    // 부정행위 검출 −50. 강퇴 −15 를 중복 부과하지 않는다
    CHEAT("CHEAT"),

    // 이의 인용 소급 — 정상 성공과 동일 복원
    APPEAL_RESTORE("APPEAL_RESTORE"),
    ;

    companion object {
        /**
         * 미지 값은 null — 사유 라벨만 비우고 증감폭은 그대로 보여준다. 모르는 사유를
         * 아무 라벨에나 접으면 그 행에 대해 거짓말을 하게 된다.
         */
        fun fromValue(value: String?): ScoreChangeReason? = entries.find { it.value == value }
    }
}

/** 다음 티어까지 (명세 `promotion`). 최상위([Tier.RUBY])면 null 이다. */
data class TierPromotion(
    val nextTier: Tier,
    // 다음 티어 시작점까지 남은 점수
    val pointsToPromote: Int,
)

/**
 * 강등 경계 (명세 `demotion`). [Tier.BRONZE] 면 null 이다 — 더 내려갈 곳이 없다.
 *
 * 표시 티어 시작점보다 1~20점 낮은 구간에서는 표시 티어를 유지하고([graceFloor] 까지),
 * 21점 이상 낮아지면([demoteAt] 이하) 강등이 확정된다(정책 §1.2).
 */
data class TierDemotion(
    // 표시 티어 시작점 −20 (유예 하한)
    val graceFloor: Int,
    // 시작점 −21. 이 점수 이하면 강등 확정
    val demoteAt: Int,
)

/**
 * 점수 변동 1건. `GET /me/tier` 의 `recentChanges[]`(최근 10건)와
 * `GET /me/tier/changes`(전체 목록)가 **같은 구조**를 쓴다.
 *
 * [challengeTitle] 은 명세 2026-09-07 신규다. **null 일 수 있다** — 완료된 방은 원본이 하드
 * 삭제되어 이력 테이블에서 읽고, 그것도 없으면 서버가 이름을 못 채운다. 화면은 그때 사유와
 * 변동폭만 그린다.
 */
data class ScoreChange(
    // YYYY-MM-DD (KST)
    val date: String,
    val reason: ScoreChangeReason?,
    // 변동을 일으킨 챌린지. 계정 단위 사유(가입 10점 등)면 null
    val challengeId: String?,
    val challengeTitle: String?,
    val delta: Int,
)

/**
 * 점수 변동 이력 한 페이지 (명세: GET /me/tier/changes — 2026-09-07 신규).
 *
 * `GET /me/tier/history` 와 **다른 API 다.** 그쪽은 그래프 원천(월말 스냅샷)이라 사유·변동폭이
 * 없다. 그래프는 기간으로 읽고 이력은 건수로 읽어 페이징 단위가 달라 합치지 않는다.
 *
 * 페이지 크기는 서버 고정 50이라 인자로 받지 않는다. [nextCursor] 가 null 이면 마지막이다.
 */
data class ScoreChangePage(
    val items: List<ScoreChange>,
    val nextCursor: String?,
    // 보관 일수 — 정책 §5 로 365. 목록 끝의 "1년치만 보관해요" 문구에 쓴다
    val retentionDays: Int?,
)

/**
 * 내 티어 상세 (명세: GET /me/tier). 구 매너 온도(`me/reputation`)를 전면 대체한다.
 *
 * [tier] 는 실제 티어, [displayTier] 는 **표시·방 입장 판정에 쓰는 티어**다. 유예 밴드에서는 둘이
 * 달라지므로 화면·게이팅은 반드시 [displayTier] 를 본다.
 *
 * 주간 변동(`weeklyDelta`)은 명세에서 폐기됐다 — 점수 한도가 챌린지별 사이클 순변동으로 확정되면서
 * '계정 주간'이라는 단위가 사라졌다(정책 §4.7).
 */
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
    /**
     * [displayTier] 구간 안에서의 진행률 0.0~1.0. 진행바 폭에만 쓴다.
     *
     * 유예 밴드에서는 점수가 표시 티어 시작점보다 낮아 음수가 되므로 0 으로 잘라 낸다 — 바를
     * 거꾸로 그리는 것보다 "아직 못 채웠다"로 보이는 편이 사실에 가깝다.
     */
    val progressInDisplayTier: Float
        get() {
            val span = displayTier.maxScore - displayTier.minScore
            if (span <= 0) return 1f
            return ((score - displayTier.minScore).toFloat() / span).coerceIn(0f, 1f)
        }
}

/** 월말 스냅샷 (명세 `monthly[]`). 하락 사유는 내려오지 않는다 — 정책상 표기하지 않는다. */
data class TierSnapshot(
    // YYYY-MM
    val month: String,
    val endTier: Tier,
    val endScore: Int,
)

/** 역대 최고 (명세 `best`). 보관 1년 범위 안에서의 최고값이다. */
data class TierBest(
    val tier: Tier,
    val score: Int,
    // YYYY-MM-DD
    val date: String,
)

/**
 * 티어 히스토리 (명세: GET /me/tier/history). 그래프 원천 데이터다.
 *
 * 구 `reputation/history` 의 마일스톤 피드는 정책 근거가 없어 폐기됐고 역대 최고만 남았다.
 * 보관은 **1년** — 그 이전 이력은 서버에서 삭제되어 조회되지 않는다.
 */
data class TierHistory(
    val best: TierBest?,
    val monthly: List<TierSnapshot>,
    val retentionNote: String?,
)
