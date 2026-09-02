package com.ruleup.report.domain.entity

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockListTest {
    private fun user(id: String = "u-1") = BlockedUser(id, "차단한 사용자", blockedAt = null)

    private fun challenge(id: String = "c-1") = BlockedChallenge(id, "가려진 챌린지", participating = false, blockedAt = null)

    @Test
    fun `양쪽이 모두 비어야 빈 목록이다`() {
        assertTrue(BlockList(users = emptyList(), challenges = emptyList()).isEmpty)
    }

    @Test
    fun `사용자만 차단해도 빈 목록이 아니다`() {
        // 한쪽만 보고 빈 상태 문구를 띄우면 차단한 사람이 있는데 없다고 말하게 된다.
        assertFalse(BlockList(users = listOf(user()), challenges = emptyList()).isEmpty)
    }

    @Test
    fun `챌린지만 차단해도 빈 목록이 아니다`() {
        assertFalse(BlockList(users = emptyList(), challenges = listOf(challenge())).isEmpty)
    }
}
