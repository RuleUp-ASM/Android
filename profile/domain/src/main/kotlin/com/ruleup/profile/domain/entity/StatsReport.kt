package com.ruleup.profile.domain.entity

/** 연속 성공. */
data class StatsStreak(
    val current: Int,
    val best: Int,
)

/** 통계 리포트. */
data class StatsReport(
    // 전체 성공률 0.0~1.0 (방 랭킹과 동일 산식)
    val successRate: Double?,
    val totalSuccessCount: Int,
    val streak: StatsStreak,
    // 완주 = 기간 중 80% 이상 성공
    val completedCount: Int,
)
