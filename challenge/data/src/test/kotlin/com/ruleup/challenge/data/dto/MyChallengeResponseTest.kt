package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.LeftType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 내 챌린지 목록 매핑. 이탈 방식과 기간은 **사용자의 지난 기록을 설명하는 값**이라, 여기서 잘못
 * 접으면 강퇴당한 사람에게 스스로 나갔다고 말하거나 기간이 빈칸이 된다.
 */
class MyChallengeResponseTest {
    @Test
    fun `모르는 이탈 방식은 스스로 나간 것으로 접지 않는다`() {
        val challenge = MyChallengeResponse(challengeId = "c1", title = "기상", leftType = "KICK_UNKNOWN").toDomain()

        assertNull(challenge.leftType)
    }

    @Test
    fun `폐지된 이탈 방식도 이미 적재된 값이라 그대로 읽는다`() {
        // 신고 강퇴는 폐지됐지만 과거 데이터가 그대로 내려온다 — 못 읽으면 그 행이 통째로 흐려진다.
        val challenge = MyChallengeResponse(challengeId = "c1", title = "기상", leftType = "KICK_REPORT").toDomain()

        assertEquals(LeftType.KICK_REPORT, challenge.leftType)
    }

    @Test
    fun `기간을 최상위로 주든 period 객체로 주든 같은 값으로 읽는다`() {
        // 명세는 최상위 startDate·endDate 인데 구 계약은 period 객체다. 어느 쪽이 와도 빈칸이면 안 된다.
        val flat = MyChallengeResponse(challengeId = "c1", title = "기상", startDate = "2026-06-02", endDate = "2026-07-13").toDomain()
        val nested =
            MyChallengeResponse(
                challengeId = "c1",
                title = "기상",
                period = PeriodDto(start = "2026-06-02", end = "2026-07-13"),
            ).toDomain()

        assertEquals(nested.period.start, flat.period.start)
        assertEquals("2026-07-13", flat.period.end)
    }

    @Test
    fun `주간 횟수를 안 주면 판정이 없는 방처럼 보이지 않게 1 로 둔다`() {
        val challenge = MyChallengeResponse(challengeId = "c1", title = "기상", weeklyCount = null).toDomain()

        assertEquals(1, challenge.weeklyCount)
    }

    @Test
    fun `다음 커서가 있으면 hasNext 를 안 줘도 다음 장이 있는 것으로 본다`() {
        // 플래그만 믿으면 서버가 빠뜨렸을 때 사용자의 지난 방이 목록에서 잘린다.
        val page = MyChallengesResponse(challenges = emptyList(), nextCursor = "c2", hasNext = null).toDomain()

        assertEquals(true, page.hasNext)
    }
}
