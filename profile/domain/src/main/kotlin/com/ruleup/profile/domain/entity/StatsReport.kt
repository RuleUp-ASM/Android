package com.ruleup.profile.domain.entity

/** 연속 성공 (명세 `streak`). 그날 예정 판정을 전부 성공해야 유지되고 하나라도 실패하면 리셋된다. */
data class StatsStreak(
    val current: Int,
    val best: Int,
)

/**
 * 통계 리포트 (명세: GET /me/stats). 정책이 정한 **지표 4종 고정**이라 기간 파라미터가 없다
 * (구 WEEKLY/MONTHLY/YEARLY 폐기).
 *
 * 구 `cycles12w`(최근 12주 사이클)와 `weeklyScoreDelta` 는 **응답·화면 계약에서 제거됐다**
 * (2026-09-15). 서버가 내려주지 않는 값을 자리만 남겨 두면 빈 그리드가 계속 노출된다(MY-07).
 *
 * [successRate] 는 판정 이력이 없으면 **null 이다 — 0 으로 접지 않는다.** 표본이 없는 것과 0% 는
 * 다른 사실이라, 0% 로 바꾸면 갓 가입한 사용자가 전부 실패한 것처럼 보인다.
 */
data class StatsReport(
    // 전체 성공률 0.0~1.0 (방 랭킹과 동일 산식)
    val successRate: Double?,
    val totalSuccessCount: Int,
    val streak: StatsStreak,
    // 완주 = 기간 중 80% 이상 성공
    val completedCount: Int,
)
