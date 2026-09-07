package com.ruleup.profile.data.dto

import com.ruleup.profile.domain.entity.CycleResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 통계 응답 매핑. 여기서 없는 값을 0 으로 접으면 **아무것도 안 한 사람과 전부 실패한 사람이
 * 같은 화면**이 된다.
 */
class StatsResponseMappingTest {
    @Test
    fun `성공률을 안 주면 0퍼센트로 접지 않는다`() {
        // 판정 이력이 없는 것과 전부 실패한 것은 다른 사실이다.
        val stats = StatsResponse(successRate = null).toDomain()

        assertNull(stats.successRate)
    }

    @Test
    fun `모르는 사이클 결과는 성공도 실패도 아닌 채로 둔다`() {
        val stats = StatsResponse(cycles12w = listOf(CycleWeekResponse(week = "2026-W28", result = "SKIPPED"))).toDomain()

        assertNull(stats.cycles12w.single().result)
    }

    @Test
    fun `주차 없는 사이클은 그리드에 올리지 않는다`() {
        val stats = StatsResponse(cycles12w = listOf(CycleWeekResponse(week = null, result = "SUCCESS"))).toDomain()

        assertTrue(stats.cycles12w.isEmpty())
    }

    @Test
    fun `연속 성공을 통째로 안 주면 0 으로 채운다`() {
        val stats = StatsResponse(streak = null).toDomain()

        assertEquals(0, stats.streak.current)
        assertEquals(0, stats.streak.best)
    }

    @Test
    fun `받은 지표 5종은 그대로 전한다`() {
        val stats =
            StatsResponse(
                successRate = 0.87,
                totalSuccessCount = 142,
                streak = StatsStreakResponse(current = 6, best = 21),
                cycles12w = listOf(CycleWeekResponse(week = "2026-W27", result = "SUCCESS")),
                completedCount = 24,
                weeklyScoreDelta = 5,
            ).toDomain()

        assertEquals(0.87, stats.successRate)
        assertEquals(142, stats.totalSuccessCount)
        assertEquals(21, stats.streak.best)
        assertEquals(CycleResult.SUCCESS, stats.cycles12w.single().result)
        assertEquals(24, stats.completedCount)
        assertEquals(5, stats.weeklyScoreDelta)
    }
}
