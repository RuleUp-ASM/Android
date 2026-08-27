package com.ruleup.profile.domain.entity

/** 통계 기간 (명세 period 쿼리 — 기본 MONTHLY). */
enum class StatsPeriod(
    val value: String,
    val label: String,
) {
    WEEKLY("WEEKLY", "주간"),
    MONTHLY("MONTHLY", "월간"),
    YEARLY("YEARLY", "연간"),
}

/** 완주율 시리즈 한 점 (bucket 포맷: 주간=일별 날짜, 월간=`W1..`, 연간=`1월..`). */
data class StatsPoint(
    val bucket: String,
    // 완주율 (%)
    val completionRate: Int,
)

/** 통계 리포트 (명세: GET /me/stats). 기간 집계는 서버 온디맨드 — 클라 재계산 없음. */
data class StatsReport(
    val period: StatsPeriod,
    // 기간 내 완주 챌린지 수
    val totalCompleted: Int,
    // 평균 완주율 (%)
    val avgCompletionRate: Int,
    // 기간 내 온도 변화 (스냅샷 diff)
    val mannerDelta: Double,
    // 평균 연속 성공일
    val avgStreak: Double,
    val series: List<StatsPoint>,
    // 규칙 기반 인사이트 1줄.
    val insight: String?,
)
