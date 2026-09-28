package com.ruleup.verification.data.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncGateTest {
    @Test
    fun `드레인 중인 동안 두 번째 진입은 거부된다`() {
        val gate = SyncGate()

        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())
    }

    @Test
    fun `앞 실행이 끝나면 다시 진입할 수 있다`() {
        val gate = SyncGate()

        assertTrue(gate.tryEnter())
        gate.leave()

        assertTrue(gate.tryEnter())
    }
}
