package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.FailureReason
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
