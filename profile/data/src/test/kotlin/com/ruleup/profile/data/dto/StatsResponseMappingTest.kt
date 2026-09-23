package com.ruleup.profile.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `연속 성공을 통째로 안 주면 0 으로 채운다`() {
        val stats = StatsResponse(streak = null).toDomain()

        assertEquals(0, stats.streak.current)
        assertEquals(0, stats.streak.best)
    }

    @Test
    fun `받은 지표 4종은 그대로 전한다`() {
        // 구 cycles12w · weeklyScoreDelta 는 응답 계약에서 빠졌다(2026-09-15).
        val stats =
            StatsResponse(
                successRate = 0.87,
                totalSuccessCount = 142,
                streak = StatsStreakResponse(current = 6, best = 21),
                completedCount = 24,
            ).toDomain()

        assertEquals(0.87, stats.successRate)
        assertEquals(142, stats.totalSuccessCount)
        assertEquals(21, stats.streak.best)
        assertEquals(24, stats.completedCount)
    }
}
