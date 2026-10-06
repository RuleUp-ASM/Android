package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 오늘 인증 카드의 진행 막대가 읽는 비율. */
class TodayResultProgressTest {
    @Test
    fun `값과 목표가 있는 근거는 그 비율로 막대를 채운다`() {
        assertEquals(0.52, today("걸음 3,120 / 목표 6,000").evidenceProgress)
        assertEquals(0.7, today("체류 42분 / 목표 60분").evidenceProgress)
    }

    @Test
    fun `목표를 넘겨도 막대는 가득 찬 데서 멈춘다`() {
        assertEquals(1.0, today("걸음 8,000 / 목표 6,000").evidenceProgress)
    }

    @Test
    fun `목표가 없거나 0 인 근거에는 막대를 그리지 않는다`() {
        // 막대가 0% 로 보이면 하나도 안 한 것처럼 읽힌다.
        assertNull(today("첫 잠금 해제 06:12").evidenceProgress)
        assertNull(today("걸음 3,120 / 목표 0").evidenceProgress)
        assertNull(today(null).evidenceProgress)
    }

    private fun today(evidence: String?) =
        TodayResult(
            date = "2026-10-06",
            verificationId = null,
            status = TodayResultStatus.IN_PROGRESS,
            window = null,
            confirmedAt = null,
            failureReason = null,
            streak = null,
            unacknowledged = null,
            appeal = null,
            evidenceSummary = evidence,
        )
}
