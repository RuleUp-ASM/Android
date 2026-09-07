package com.ruleup.profile.domain.entity

/**
 * 주 단위 사이클 결과 (명세 `cycles12w[].result`).
 *
 * [NONE] 은 실패가 아니라 **판정 자체가 없던 주**다 — 가입 전이거나 참여 중인 챌린지가 없던 주라서,
 * 실패와 같은 색으로 칠하면 하지도 않은 실패를 12주 그리드에 새기게 된다.
 */
enum class CycleResult(
    val value: String,
) {
    SUCCESS("SUCCESS"),
    PARTIAL("PARTIAL"),
    FAIL("FAIL"),
    NONE("NONE"),
    ;

    companion object {
        /** 미지 값은 null — 그 칸을 비워 둔다. 모르는 결과를 성공·실패 어느 쪽으로도 접지 않는다. */
        fun fromValue(value: String?): CycleResult? = entries.find { it.value == value }
    }
}

/** 12주 그리드의 한 칸 (명세 `cycles12w[]`). */
data class CycleWeek(
    // ISO 주차 (예: "2026-W28")
    val week: String,
    val result: CycleResult?,
)

/** 연속 성공 (명세 `streak`). 그날 예정 판정을 전부 성공해야 유지되고 하나라도 실패하면 리셋된다. */
data class StatsStreak(
    val current: Int,
    val best: Int,
)

/**
 * 통계 리포트 (명세: GET /me/stats). 정책이 정한 **지표 5종 고정**이라 기간 파라미터가 없다
 * (구 WEEKLY/MONTHLY/YEARLY 폐기).
 *
 * [successRate] 는 판정 이력이 없으면 **null 이다 — 0 으로 접지 않는다.** 표본이 없는 것과 0% 는
 * 다른 사실이라, 0% 로 바꾸면 갓 가입한 사용자가 전부 실패한 것처럼 보인다.
 */
data class StatsReport(
    // 전체 성공률 0.0~1.0 (방 랭킹과 동일 산식)
    val successRate: Double?,
    val totalSuccessCount: Int,
    val streak: StatsStreak,
    val cycles12w: List<CycleWeek>,
    // 완주 = 기간 중 80% 이상 성공
    val completedCount: Int,
    // 이번 주 점수 변동 (사이클분 상한 ±15). 명세가 "수치 미확정"이라 없으면 표기를 생략한다
    val weeklyScoreDelta: Int?,
)
