package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category

/** 홈 화면 즉시 반영용 "내 챌린지" 로컬 요약. */
data class MyChallengeSummary(
    val challengeId: String,
    val title: String,
    val category: Category?,
    val mode: ChallengeMode,
    // 기간 길이(일).
    val durationDays: Int,
)
