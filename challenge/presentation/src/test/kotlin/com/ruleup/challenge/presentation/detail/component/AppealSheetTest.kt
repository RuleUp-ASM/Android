package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.AppealChance
import com.ruleup.verification.domain.entity.AppealPolicy
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppealSheetTest {
    @Test
    fun `하한을 넘기면 카운터가 다음 행동을 알린다`() {
        // 다 쓰고 제출을 눌러야 되는지 알 수 있으면 400 왕복이 생긴다.
        assertEquals("28자 · 제출할 수 있어요", reasonCounter(length = 28, enough = true))
        assertEquals("${AppealPolicy.MIN_REASON_LENGTH}자 이상 적어 주세요", reasonCounter(length = 3, enough = false))
    }

    @Test
    fun `비공개 고지는 실익과 마감일을 함께 말한다`() {
        // "공개되지 않았다"만 있으면 지금 이의할 이유가 드러나지 않는다.
        val notice = today(eligibleUntil = "2026-07-27T00:00:00+09:00").privacyNotice()

        assertTrue(notice.contains("아직 그룹에 공개되지 않았어요"))
        assertTrue(notice.contains("까지 이의하면 공개되지 않아요"))
    }

    @Test
    fun `마감 시각을 모르면 날짜를 지어내지 않는다`() {
        val today = today(eligibleUntil = null)

        assertNull(today.appealDeadlineText())
        assertTrue(today.privacyNotice().contains("지금 이의하면"))
    }

    @Test
    fun `마감 안내는 경계 시각이 아니라 낼 수 있는 마지막 날을 쓴다`() {
        // eligibleUntil 은 다음 날 00:00 경계라 그대로 쓰면 하루 늦게 안내한다.
        val text = today(eligibleUntil = "2026-07-27T00:00:00+09:00").appealDeadlineText()

        assertTrue(text!!.endsWith("까지"))
        assertTrue(!text.contains("27"))
    }

    private fun today(eligibleUntil: String?): TodayResult =
        TodayResult(
            date = "2026-07-26",
            verificationId = "v_1",
            status = TodayResultStatus.FAILED,
            window = null,
            confirmedAt = null,
            failureReason = null,
            streak = null,
            unacknowledged = null,
            appeal = AppealChance(eligibleUntil = eligibleUntil, eligible = true),
        )
}
