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

    @Test
    fun `인증 방법 안내를 그대로 옮긴다`() {
        val response =
            ChallengeDetailResponse(challengeId = "c1", verification = VerificationResponse(guide = "매일 10,000걸음 이상 걸으면 자동 인증됩니다."))
        assertEquals("매일 10,000걸음 이상 걸으면 자동 인증됩니다.", response.toDomain().verification.guide)
    }

    @Test
    fun `인증 방법 안내가 아직 없으면 비워 둔다`() {
        // 서버가 채우는 중이다. 화면이 「아직 입력중입니다.」로 가른다.
        assertNull(ChallengeDetailResponse(challengeId = "c1", verification = VerificationResponse()).toDomain().verification.guide)
    }
}
