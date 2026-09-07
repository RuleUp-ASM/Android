package com.ruleup.report.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReportTargetTest {
    @Test
    fun `챌린지 신고는 부정 인증 의심 사유를 거부한다`() {
        // 서버가 400 INVALID_REPORT_REASON 으로 막는 조합이다. 여기서 안 막으면 사용자가
        // 사유를 고르고 전송까지 누른 뒤에야 실패를 본다.
        assertFailsWith<IllegalArgumentException> {
            ReportTarget.Challenge(
                challengeId = "c-1",
                reason = ReportReason.CHEATING_SUSPECT,
                context = ReportContext.CHALLENGE_DETAIL,
            )
        }
    }

    @Test
    fun `챌린지 신고는 나머지 세 사유를 모두 받는다`() {
        ReportReason.forChallenge.forEach { reason ->
            ReportTarget.Challenge("c-1", reason, ReportContext.CHALLENGE_DETAIL)
        }
    }

    @Test
    fun `프로필 밖에서 하는 사용자 신고는 발생한 챌린지가 없으면 만들어지지 않는다`() {
        // 스냅샷에 방 정보가 없으면 운영자가 무슨 행위였는지 판단할 수 없어 서버가 거절한다.
        listOf(ReportContext.CHALLENGE_DETAIL, ReportContext.ROOM).forEach { context ->
            assertFailsWith<IllegalArgumentException>("$context 에서 챌린지 없이 통과했다") {
                ReportTarget.User("u-1", ReportReason.SPAM_AD, context)
            }
        }
    }

    @Test
    fun `프로필에서 하는 사용자 신고는 챌린지 없이도 성립한다`() {
        val target = ReportTarget.User("u-1", ReportReason.SPAM_AD, ReportContext.PROFILE)

        assertEquals(null, target.challengeId)
    }

    @Test
    fun `사용자 신고는 네 사유를 모두 고를 수 있다`() {
        assertEquals(ReportReason.entries, ReportReason.forUser)
    }

    @Test
    fun `챌린지에서 빠지는 사유는 부정 인증 의심 하나뿐이다`() {
        assertEquals(
            ReportReason.entries - ReportReason.CHEATING_SUSPECT,
            ReportReason.forChallenge,
        )
        assertTrue(ReportReason.CHEATING_SUSPECT !in ReportReason.forChallenge)
    }
}
