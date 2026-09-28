package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 챌린지 월 캘린더. */
class ChallengeDayStatusTest {
    @Test
    fun `명세의 4종만 정의돼 있고 서버 값과 이름이 같다`() {
        assertEquals(
            listOf("DONE", "FAILED", "FAIL_EXPECTED", "IN_PROGRESS"),
            ChallengeDayStatus.entries.map { it.value },
        )
    }

    @Test
    fun `모르는 상태는 실패로 접지 않고 칸을 비운다`() {
        // 서버가 상태를 하나 늘렸을 뿐인데 사용자에게 실패했다고 말하면 안 된다.
        assertNull(ChallengeDayStatus.fromValue("SKIPPED"))
        assertNull(ChallengeDayStatus.fromValue(null))
    }

    @Test
    fun `유예 창은 확정된 실패가 아니다`() {
        assertFalse(ChallengeDayStatus.FAIL_EXPECTED.isSettledFailure)
        assertTrue(ChallengeDayStatus.FAILED.isSettledFailure)
    }
}

class ChallengeCalendarTest {
    @Test
    fun `판정 대상이 아닌 날은 기록이 없다`() {
        assertNull(calendar().dayOf("2026-09-02"))
    }

    @Test
    fun `판정 대상일은 날짜로 찾아진다`() {
        assertEquals(ChallengeDayStatus.DONE, calendar().dayOf("2026-09-01")?.status)
    }

    private fun calendar() =
        ChallengeCalendar(
            challengeId = "c1",
            month = "2026-09",
            days =
                listOf(
                    ChallengeCalendarDay(
                        date = "2026-09-01",
                        status = ChallengeDayStatus.DONE,
                        verificationId = "v1",
                        appealable = false,
                    ),
                ),
        )
}
