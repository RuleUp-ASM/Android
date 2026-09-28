package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeSignalTest {
    @Test
    fun `잠금해제도 화면 켜짐도 없으면 보낼 것이 없다`() {
        val wake = VerificationSignal.Wake(firstUnlock = null, firstScreenOn = null, deviceSecure = true)

        assertTrue(wake.isEmpty)
    }

    @Test
    fun `화면 켜짐만 있어도 보낼 것이 있다`() {
        val wake = VerificationSignal.Wake(firstUnlock = null, firstScreenOn = 1L, deviceSecure = false)

        assertFalse(wake.isEmpty)
    }
}
