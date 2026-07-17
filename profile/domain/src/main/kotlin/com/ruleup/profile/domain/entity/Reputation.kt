package com.ruleup.profile.domain.entity

/** 다음 온도 목표 (명세 nextTier — 앵커 50/60/70/75/80/85/90 중 현재보다 큰 최소값). */
data class NextTier(
    // 다음 앵커 온도
    val target: Double,
    // 이전 앵커 → 다음 앵커 구간 진행률 (0.0~1.0)
    val progressRate: Double,
    // 달성 안내 문구 (예: "80℃ 달성 시 프리미엄 챌린지 참여 가능")
    val label: String?,
)

/** 일별 온도 변동 (명세 recentChanges[] — 서버 고정 최근 10건, 배치가 하루 1스텝). */
data class ReputationChange(
    // YYYY-MM-DD (KST)
    val date: String,
    // 그날 배치 후 온도
    val temperature: Double,
    // 전일 대비 변동 (+/-)
    val delta: Double,
    // 규칙 라벨 (예: "자격일 유지" / "페이스 하락")
    val label: String,
)

/** 매너 온도 상세 (명세: GET /me/reputation). */
data class ReputationDetail(
    val current: Double,
    // 현재 밴드 문구 (온도 스펙 밴드 표 매핑)
    val bandLabel: String,
    // 최상위 밴드면 null (더 오를 목표 없음)
    val nextTier: NextTier?,
    val recentChanges: List<ReputationChange>,
)

/** 마일스톤 종류 (명세 milestones[].type). 미지 값은 [ETC] 로 흡수해 렌더만 한다. */
enum class MilestoneType(
    val value: String,
) {
    TIER_REACHED("TIER_REACHED"),
    STREAK("STREAK"),
    FIRST_COMPLETION("FIRST_COMPLETION"),
    SIGNUP("SIGNUP"),
    ETC(""),
    ;

    companion object {
        fun fromValue(value: String?): MilestoneType = entries.find { it.value == value } ?: ETC
    }
}

/** 마일스톤 항목 (append-only 피드 — 시간 역순, 서버 상한 50건). */
data class ReputationMilestone(
    val type: MilestoneType,
    val label: String,
    // 달성일 (YYYY-MM-DD)
    val achievedAt: String,
)

/** 평판 히스토리 (명세: GET /me/reputation/history). */
data class ReputationHistory(
    // 역대 최고 온도
    val peakTemperature: Double,
    val peakAchievedAt: String,
    val milestones: List<ReputationMilestone>,
)
