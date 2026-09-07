package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 챌린지 월 캘린더. 이 화면은 **없는 실패를 지어내기 가장 쉬운 자리**다 — 판정 대상이 아닌 날과
 * 아직 확정되지 않은 날이 실패와 같아 보이면, 사용자는 쉬는 날에 실패했다고 읽는다.
 */
class ChallengeDayStatusTest {
    @Test
    fun `명세의 4종만 정의돼 있고 서버 값과 이름이 같다`() {
        // /me/calendar 의 ALL_DONE·PARTIAL 이 섞여 들어오면 한 방으로 좁힌 뜻이 무너진다.
        assertEquals(
            listOf("DONE", "FAILED", "CHECKING", "IN_PROGRESS"),
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
        // 아직 뒤집힐 수 있다(인증 정책 2-1). 확정 실패와 같아 보이면 이의를 포기하게 된다.
        assertFalse(ChallengeDayStatus.CHECKING.isSettledFailure)
        assertTrue(ChallengeDayStatus.FAILED.isSettledFailure)
    }
}

class ChallengeCalendarTest {
    @Test
    fun `판정 대상이 아닌 날은 기록이 없다`() {
        // 주 3회 방이면 한 달에 12~13칸만 온다 — 빈 날짜를 실패로 채우면 안 된다.
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
