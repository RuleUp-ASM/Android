package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.PendingReason
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.VerificationStreak
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodayVerificationCopyTest {
    @Test
    fun `실패 사유 전수가 문구를 갖는다`() {
        // 사유 없이 "실패"만 남으면 사용자는 자기가 뭘 못 했는지 알 수 없다.
        FailureReason.entries.forEach { reason ->
            assertTrue(reason.failureText().isNotBlank(), "$reason 문구 누락")
        }
    }

    @Test
    fun `권한과 신호 부재는 다른 문구를 쓴다`() {
        // 사용자가 할 수 있는 조치가 다르다 — 권한 허용 대 전송 재개.
        assertTrue(
            FailureReason.PERMISSION_MISSING.failureText() != FailureReason.NO_SIGNAL_RECEIVED.failureText(),
        )
    }

    @Test
    fun `손입력 기록은 부정행위로 단정하지 않는다`() {
        // "조작"·"부정" 같은 단어를 쓰면 정상 사용자를 거짓말쟁이로 대하게 된다.
        val text = FailureReason.UNTRUSTED_HEALTH_SOURCE.failureText()
        assertTrue(text.contains("직접 입력"))
        assertTrue(!text.contains("부정") && !text.contains("조작"))
    }

    @Test
    fun `실패 카드는 사유를 한 번만 말한다`() {
        // 판정 불가와 실패 사유가 함께 와도 같은 사실을 두 번 읽히지 않는다 — 한 줄이 계속 길어지면
        // 사용자는 끝까지 안 읽고, 정작 무엇을 해야 하는지는 뒤쪽에 밀린다.
        val note =
            todayNote(
                status = TodayResultStatus.FAILED,
                today =
                    failedToday(
                        failureReason = FailureReason.INSUFFICIENT_STEPS,
                        pendingReason = PendingReason.NO_SIGNAL,
                    ),
            )

        assertEquals(FailureReason.INSUFFICIENT_STEPS.failureText(), note)
    }

    @Test
    fun `판정 근거에 섞인 서버 코드가 카드에 새지 않는다`() {
        // evidenceSummary 는 서버가 「걸음 부족 (INSUFFICIENT_STEPS)」처럼 원문 enum 을 섞어 보낸다.
        // 그대로 이어 붙이면 사용자 화면에 코드 이름이 뜬다.
        val note =
            todayNote(
                status = TodayResultStatus.FAIL_EXPECTED,
                today =
                    failedToday(
                        failureReason = FailureReason.INSUFFICIENT_STEPS,
                        evidenceSummary = "걸음 3,120 / 목표 6,000 (INSUFFICIENT_STEPS)",
                    ),
            )

        assertTrue(note != null && !note.contains("INSUFFICIENT_STEPS"))
    }

    @Test
    fun `연속이 끊겼으면 사유 뒤에 그 사실만 덧붙인다`() {
        val note =
            todayNote(
                status = TodayResultStatus.FAILED,
                today =
                    failedToday(
                        failureReason = FailureReason.WOKE_UP_LATE,
                        streak = VerificationStreak(before = 4, after = 0),
                    ),
            )

        assertEquals("${FailureReason.WOKE_UP_LATE.failureText()} · 연속 4일이 끊겼어요", note)
    }

    private fun failedToday(
        failureReason: FailureReason? = null,
        pendingReason: PendingReason? = null,
        evidenceSummary: String? = null,
        streak: VerificationStreak? = null,
    ) = TodayResult(
        date = "2026-07-26",
        verificationId = "v_1",
        status = TodayResultStatus.FAILED,
        window = null,
        confirmedAt = null,
        failureReason = failureReason,
        streak = streak,
        unacknowledged = null,
        appeal = null,
        pendingReason = pendingReason,
        evidenceSummary = evidenceSummary,
    )

    @Test
    fun `이의 마감은 경계 시각이 아니라 낼 수 있는 마지막 날로 안내한다`() {
        // eligibleUntil 은 경계 시각이다. 그 날짜를 그대로 쓰면 이의 가능일을 하루 늦게 안내한다.
        val today = LocalDate.of(2026, 7, 26)

        assertEquals("오늘", appealDeadlineLabel("2026-07-27T00:00:00+09:00", today))
        assertEquals("내일", appealDeadlineLabel("2026-07-28T00:00:00+09:00", today))
        assertEquals("7월 30일", appealDeadlineLabel("2026-07-31T00:00:00+09:00", today))
    }

    @Test
    fun `마감 시각을 못 읽으면 날짜를 지어내지 않는다`() {
        assertNull(appealDeadlineLabel("nonsense", LocalDate.of(2026, 7, 26)))
    }
}
