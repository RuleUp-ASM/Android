package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.AppealChance
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import com.ruleup.verification.domain.entity.VerificationStreak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerificationResultModalTest {
    @Test
    fun `성공은 연속 기록이 어떻게 바뀌었는지 말한다`() {
        val today = today(result = "DONE", streak = VerificationStreak(before = 6, after = 7))

        assertFalse(today.isFailedResult())
        assertEquals("6일 → 7일 연속", resultNote(today, failed = false))
    }

    @Test
    fun `실패는 사유를 말한다`() {
        // 사유 없이 "실패했어요"만 남으면 사용자는 뭘 못 했는지 모른 채 모달을 닫는다.
        val today = today(result = "FAILED", failureReason = FailureReason.WOKE_UP_LATE)

        assertTrue(today.isFailedResult())
        assertEquals(FailureReason.WOKE_UP_LATE.failureText(), resultNote(today, failed = true))
    }

    @Test
    fun `연속 기록이 없으면 성공 문구를 지어내지 않는다`() {
        val today = today(result = "DONE", streak = null)

        assertNull(resultNote(today, failed = false))
    }

    @Test
    fun `이의 안내는 마감일을 함께 말한다`() {
        // "이의할 수 있어요"만 있으면 언제까지인지 몰라 그냥 닫는다.
        val today =
            today(
                result = "FAILED",
                appeal = AppealChance(eligibleUntil = "2026-07-27T00:00:00+09:00", eligible = true),
            )

        assertTrue(today.appealHint().contains("까지"))
    }

    private fun today(
        result: String,
        streak: VerificationStreak? = null,
        failureReason: FailureReason? = null,
        appeal: AppealChance? = null,
    ): TodayResult =
        TodayResult(
            date = "2026-07-26",
            verificationId = "v_1",
            status = if (result == "FAILED") TodayResultStatus.FAILED else TodayResultStatus.DONE,
            window = null,
            confirmedAt = null,
            failureReason = failureReason,
            streak = streak,
            unacknowledged = UnacknowledgedResult(verificationId = "v_1", result = result),
            appeal = appeal,
        )
}
