package com.ruleup.verification.data.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncGateTest {
    @Test
    fun `드레인 중인 동안 두 번째 진입은 거부된다`() {
        // 이게 깨지면 주기 work 와 catch-up 이 같은 버퍼를 겹쳐 밟아 같은 신호가 두 번 나간다(#355).
        val gate = SyncGate()

        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())
    }

    @Test
    fun `앞 실행이 끝나면 다시 진입할 수 있다`() {
        // leave 가 안 풀리면 첫 실행 이후 sync 가 영구히 멈춘다 — Worker 의 finally 가 지켜야 하는 계약.
        val gate = SyncGate()

        assertTrue(gate.tryEnter())
        gate.leave()

        assertTrue(gate.tryEnter())
    }
}
