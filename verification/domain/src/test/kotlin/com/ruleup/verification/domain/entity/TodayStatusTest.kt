package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodayStatusTest {
    @Test
    fun `진행률 오늘 상태는 오늘 결과 조회와 같은 5종이다`() {
        // 구 SUCCESS·PENDING 어휘가 남으면 서버가 보내는 DONE 이 진행 중으로 보인다.
        assertEquals(
            listOf("IN_PROGRESS", "FAIL_EXPECTED", "DONE", "FAILED", "NOT_TARGET"),
            TodayStatus.entries.map { it.name },
        )
    }

    @Test
    fun `확정된 FAILED 만 진행률 실패 상태다`() {
        // 실패 예정은 늦은 신호로 뒤집힐 수 있고 NOT_TARGET 은 애초에 대상이 아니다.
        assertTrue(TodayStatus.FAILED.isFailure)
        assertFalse(TodayStatus.FAIL_EXPECTED.isFailure)
        assertFalse(TodayStatus.IN_PROGRESS.isFailure)
        assertFalse(TodayStatus.NOT_TARGET.isFailure)
        assertFalse(TodayStatus.DONE.isFailure)
    }

    @Test
    fun `모르는 진행률 상태는 진행 중으로 접지 않는다`() {
        assertNull(TodayStatus.fromValue("SOMETHING_NEW"))
    }
}
