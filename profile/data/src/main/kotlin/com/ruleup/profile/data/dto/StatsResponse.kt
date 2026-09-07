package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.CycleResult
import com.ruleup.profile.domain.entity.CycleWeek
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.StatsStreak
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 통계 리포트 (GET /me/stats) ----------
@Serializable
data class StatsStreakResponse(
    @SerialName("current")
    val current: Int? = null,
    @SerialName("best")
    val best: Int? = null,
)

@Serializable
data class CycleWeekResponse(
    // ISO 주차 (예: "2026-W28")
    @SerialName("week")
    val week: String? = null,
    @SerialName("result")
    val result: String? = null,
)

@Serializable
data class StatsResponse(
    // 0.0~1.0
    @SerialName("successRate")
    val successRate: Double? = null,
    @SerialName("totalSuccessCount")
    val totalSuccessCount: Int? = null,
    @SerialName("streak")
    val streak: StatsStreakResponse? = null,
    @SerialName("cycles12w")
    val cycles12w: List<CycleWeekResponse>? = null,
    @SerialName("completedCount")
    val completedCount: Int? = null,
    @SerialName("weeklyScoreDelta")
    val weeklyScoreDelta: Int? = null,
)

internal fun StatsResponse.toDomain(): StatsReport =
    StatsReport(
        // 표본이 없으면 서버가 비운다. 0 으로 접으면 "전부 실패"로 읽힌다.
        successRate = successRate?.coerceIn(0.0, 1.0),
        totalSuccessCount = totalSuccessCount ?: 0,
        streak = StatsStreak(current = streak?.current ?: 0, best = streak?.best ?: 0),
        // 주차가 없는 칸은 그리드에 세울 자리가 없다.
        cycles12w = cycles12w.orEmpty().mapNotNull { it.toDomain() },
        completedCount = completedCount ?: 0,
        weeklyScoreDelta = weeklyScoreDelta,
    )

internal fun CycleWeekResponse.toDomain(): CycleWeek? {
    val week = week ?: return null
    return CycleWeek(week = week, result = CycleResult.fromValue(result))
}
