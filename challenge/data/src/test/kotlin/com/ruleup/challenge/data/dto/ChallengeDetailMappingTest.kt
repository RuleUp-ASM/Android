package com.ruleup.challenge.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 챌린지 상세 응답 매핑. */
class ChallengeDetailMappingTest {
    @Test
    fun `주간 횟수를 그대로 옮긴다`() {
        assertEquals(3, ChallengeDetailResponse(challengeId = "c1", weeklyCount = 3).toDomain().weeklyCount)
    }

    @Test
    fun `주간 횟수가 없으면 비워 둔다`() {
        // 매일로 채우면 상세가 저장되지 않은 빈도를 단정한다.
        assertNull(ChallengeDetailResponse(challengeId = "c1").toDomain().weeklyCount)
    }

    @Test
    fun `범위를 벗어난 주간 횟수는 비워 둔다`() {
        assertNull(ChallengeDetailResponse(challengeId = "c1", weeklyCount = 0).toDomain().weeklyCount)
        assertNull(ChallengeDetailResponse(challengeId = "c1", weeklyCount = 8).toDomain().weeklyCount)
    }
}
