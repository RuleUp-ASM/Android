package com.ruleup.verification.data.signal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageSignalMappingTest {
    // 코드 값: ACTIVITY_RESUMED=1, ACTIVITY_PAUSED=2, ACTIVITY_STOPPED=23, KEYGUARD_HIDDEN=18, SCREEN_INTERACTIVE=15
    @Test
    fun `RESUMED(1)은 APP RESUMED`() {
        val mapping = usageMappingOf(1)
        assertEquals("APP", mapping?.kind)
        assertEquals("RESUMED", mapping?.eventType)
    }

    @Test
    fun `ACTIVITY_STOPPED(23)은 APP STOPPED`() {
        val mapping = usageMappingOf(23)
        assertEquals("APP", mapping?.kind)
        assertEquals("STOPPED", mapping?.eventType)
    }

    @Test
    fun `KEYGUARD_HIDDEN(18)은 SCREEN UNLOCK - WAKE 판정용`() {
        val mapping = usageMappingOf(18)
        assertEquals("SCREEN", mapping?.kind)
        assertEquals("UNLOCK", mapping?.eventType)
    }

    @Test
    fun `수집 대상 아닌 코드는 null 로 버린다`() {
        assertNull(usageMappingOf(7))
    }
}
