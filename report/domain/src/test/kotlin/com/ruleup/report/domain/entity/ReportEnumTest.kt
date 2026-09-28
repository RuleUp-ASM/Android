package com.ruleup.report.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportEnumTest {
    @Test
    fun `신고 사유 와이어 값은 명세의 4종이다`() {
        assertEquals(
            listOf("CHEATING_SUSPECT", "INAPPROPRIATE", "SPAM_AD", "ETC"),
            ReportReason.entries.map { it.value },
        )
    }

    @Test
    fun `신고 진입 화면 와이어 값은 명세의 3종이다`() {
        assertEquals(
            listOf("PROFILE", "CHALLENGE_DETAIL", "ROOM"),
            ReportContext.entries.map { it.value },
        )
    }

    @Test
    fun `가림 효과 와이어 값은 명세의 3종이다`() {
        assertEquals(
            listOf("USER_CONTENT_MASKED", "CHALLENGE_HIDDEN", "CHALLENGE_MASKED"),
            HiddenEffect.entries.map { it.value },
        )
    }

    @Test
    fun `모르는 가림 효과는 null 로 흘려보낸다`() {
        assertNull(HiddenEffect.fromValue("SOMETHING_NEW"))
        assertNull(HiddenEffect.fromValue(null))
    }

    @Test
    fun `가림 효과는 와이어 값으로 되찾을 수 있다`() {
        HiddenEffect.entries.forEach { effect ->
            assertEquals(effect, HiddenEffect.fromValue(effect.value))
        }
    }
}
