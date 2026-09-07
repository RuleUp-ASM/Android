package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WaitlistPolicyTest {
    @Test
    fun `대기 한도는 정원의 50퍼센트이고 소수점은 버린다`() {
        // 한도를 화면과 서버가 따로 계산하면 "버튼은 열렸는데 요청은 막히는" 상태가 생긴다.
        assertEquals(5, WaitlistPolicy.maxSize(10))
        assertEquals(2, WaitlistPolicy.maxSize(5))
        assertEquals(1, WaitlistPolicy.maxSize(3))
    }

    @Test
    fun `정원 1인 방은 대기열이 없다`() {
        // 내림 해석의 귀결이다. 정책이 바뀌면 WaitlistPolicy 한 곳만 고친다.
        assertEquals(0, WaitlistPolicy.maxSize(1))
    }
}

class WaitlistPositionTest {
    @Test
    fun `서버가 순번을 안 주면 확인 중이지 1번이 아니다`() {
        // null 을 1 로 접으면 "다음 차례" 라는 없는 약속이 화면에 생긴다.
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.fromValue(null))
    }

    @Test
    fun `0 이하는 순번이 아니므로 확인 중으로 떨어진다`() {
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.fromValue(0))
        assertEquals(WaitlistPosition.Calculating, WaitlistPosition.fromValue(-1))
    }

    @Test
    fun `1 이상은 확정 순번이다`() {
        assertEquals(WaitlistPosition.Assigned(1), WaitlistPosition.fromValue(1))
        assertEquals(WaitlistPosition.Assigned(7), WaitlistPosition.fromValue(7))
    }
}

class WaitlistStatusTest {
    private fun entry(position: WaitlistPosition = WaitlistPosition.Calculating) =
        WaitlistEntry(
            challengeId = "c1",
            position = position,
            waitingCount = 1,
            joinedAt = "2026-08-22T10:00:00Z",
        )

    @Test
    fun `한도 미만이면 대기열에 들어갈 수 있다`() {
        val status = WaitlistStatus(capacity = 10, waitingCount = 4, myEntry = null)

        assertFalse(status.isFull)
        assertTrue(status.canEnqueue)
    }

    @Test
    fun `한도에 도달하면 버튼이 막힌다`() {
        // 정원 10 → 대기 5명까지. 5명째가 차는 순간 더는 받지 않는다.
        val status = WaitlistStatus(capacity = 10, waitingCount = 5, myEntry = null)

        assertTrue(status.isFull)
        assertFalse(status.canEnqueue)
    }

    @Test
    fun `이미 대기 중이면 다시 넣지 않는다`() {
        val status = WaitlistStatus(capacity = 10, waitingCount = 1, myEntry = entry())

        assertTrue(status.isWaiting)
        assertFalse(status.canEnqueue)
    }
}

class WaitlistExitTest {
    @Test
    fun `대기 종료 사유 3종을 매핑한다`() {
        assertEquals(WaitlistExitReason.PROMOTED, WaitlistExitReason.fromValue("PROMOTED"))
        assertEquals(WaitlistExitReason.CHALLENGE_STARTED, WaitlistExitReason.fromValue("CHALLENGE_STARTED"))
        assertEquals(WaitlistExitReason.CANCELED, WaitlistExitReason.fromValue("CANCELED"))
        assertEquals(3, WaitlistExitReason.entries.size)
    }

    @Test
    fun `모르는 사유는 null 이다`() {
        assertNull(WaitlistExitReason.fromValue("SOMETHING_NEW"))
        assertNull(WaitlistExitReason.fromValue(null))
    }

    @Test
    fun `방이 시작되면 대기가 끝나고 참여료가 환불된다`() {
        val exit =
            WaitlistExit(
                challengeId = "c1",
                reason = WaitlistExitReason.CHALLENGE_STARTED,
                refund = WaitlistRefund.Refunded(3_000),
            )

        assertFalse(exit.reason.isPromoted)
        assertEquals(WaitlistRefund.Refunded(3_000), exit.refund)
    }

    @Test
    fun `참여료가 없던 방은 환불 안내를 그릴 근거가 없다`() {
        // "참여료 없음" 과 "0원 환불" 을 한 숫자로 접지 않는다.
        val exit =
            WaitlistExit(
                challengeId = "c1",
                reason = WaitlistExitReason.CHALLENGE_STARTED,
                refund = WaitlistRefund.NotCharged,
            )

        assertEquals(WaitlistRefund.NotCharged, exit.refund)
    }
}
