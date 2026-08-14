package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodayStatusTest {
    @Test
    fun `FAILED 만 실패 상태다`() {
        // PENDING 은 아직 판정 전이고 NOT_TARGET 은 애초에 대상이 아니다 — 둘 다 실패 카피가 붙으면 안 된다.
        assertTrue(TodayStatus.FAILED.isFailure)
        assertFalse(TodayStatus.PENDING.isFailure)
        assertFalse(TodayStatus.NOT_TARGET.isFailure)
        assertFalse(TodayStatus.SUCCESS.isFailure)
    }
}
