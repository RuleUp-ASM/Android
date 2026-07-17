package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsPoint
import com.ruleup.profile.domain.entity.StatsReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 통계 리포트 (GET /me/stats?period&anchor) ----------
@Serializable
data class StatsPointResponse(
    // 주간=일별 날짜 / 월간=W1.. / 연간=1월..
    @SerialName("bucket")
    val bucket: String? = null,
    @SerialName("completionRate")
    val completionRate: Int? = null,
)

@Serializable
data class StatsResponse(
    @SerialName("period")
    val period: String? = null,
    @SerialName("totalCompleted")
    val totalCompleted: Int? = null,
    @SerialName("avgCompletionRate")
    val avgCompletionRate: Int? = null,
    @SerialName("mannerDelta")
    val mannerDelta: Double? = null,
    @SerialName("avgStreak")
    val avgStreak: Double? = null,
    @SerialName("series")
    val series: List<StatsPointResponse>? = null,
    @SerialName("insight")
    val insight: String? = null,
)

internal fun StatsResponse.toDomain(requested: StatsPeriod): StatsReport =
    StatsReport(
        period = StatsPeriod.entries.find { it.value == period } ?: requested,
        totalCompleted = totalCompleted ?: 0,
        avgCompletionRate = avgCompletionRate ?: 0,
        mannerDelta = mannerDelta ?: 0.0,
        avgStreak = avgStreak ?: 0.0,
        series =
            series.orEmpty().map {
                StatsPoint(
                    bucket = it.bucket.orEmpty(),
                    completionRate = (it.completionRate ?: 0).coerceIn(0, 100),
                )
            },
        insight = insight?.takeIf { it.isNotBlank() },
    )
