package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JoinBlockReasonTest {
    @Test
    fun `명세의 8종을 모두 매핑한다`() {
        val expected =
            mapOf(
                "PRIVATE_INVITE_ONLY" to JoinBlockReason.PRIVATE_INVITE_ONLY,
                "REJOIN_COOLDOWN" to JoinBlockReason.REJOIN_COOLDOWN,
                "FREE_LIMIT" to JoinBlockReason.FREE_LIMIT,
                "FULL" to JoinBlockReason.FULL,
                "TIER_GATE" to JoinBlockReason.TIER_GATE,
                "PERMANENT_BAN" to JoinBlockReason.BANNED,
                "ALREADY_JOINED" to JoinBlockReason.ALREADY_JOINED,
                "CHALLENGE_COMPLETED" to JoinBlockReason.CHALLENGE_COMPLETED,
            )

        expected.forEach { (value, reason) -> assertEquals(reason, JoinBlockReason.fromValue(value)) }
        assertEquals(expected.size, JoinBlockReason.entries.size)
    }

    @Test
    fun `모르는 사유는 null 이다`() {
        // 서버가 사유를 추가해도 앱이 터지지 않고 일반 안내로 떨어져야 한다.
        assertNull(JoinBlockReason.fromValue("SOMETHING_NEW"))
        assertNull(JoinBlockReason.fromValue(null))
    }
}
