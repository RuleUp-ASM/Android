package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category

/**
 * 홈 화면 즉시 반영용 "내 챌린지" 로컬 요약.
 *
 * 생성/참여 직후 서버 진행률(verification/progress)이 아직 이 챌린지를 포함하지 않더라도
 * 홈에 바로 노출되도록 보관하는 최소 정보다. 진행률 응답과 병합돼 카드로 렌더된다.
 */
data class MyChallengeSummary(
    val challengeId: String,
    val title: String,
    val category: Category?,
    val participationType: ParticipationType,
    val durationDays: Int,
)
