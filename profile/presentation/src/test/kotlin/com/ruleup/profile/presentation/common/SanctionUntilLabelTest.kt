package com.ruleup.profile.presentation.common

import kotlin.test.Test
import kotlin.test.assertEquals

/** 제재 해제 시각 표기. */
class SanctionUntilLabelTest {
    @Test
    fun `UTC 자정을 넘기는 해제 시각을 KST로 표시한다`() {
        assertEquals("2026. 10. 15 00:00", sanctionUntilLabel("2026-10-14T15:00:00Z"))
    }

    @Test
    fun `해제 시각은 날짜와 시·분까지 보여 준다`() {
        assertEquals("2026. 10. 15 00:00", sanctionUntilLabel("2026-10-15T00:00:00+09:00"))
    }

    @Test
    fun `시각이 없으면 날짜만 보여 주고 0시를 지어내지 않는다`() {
        assertEquals("2026. 10. 15", sanctionUntilLabel("2026-10-15"))
    }

    @Test
    fun `해석할 수 없는 문자열은 그대로 내보낸다`() {
        assertEquals("곧 해제", sanctionUntilLabel("곧 해제"))
    }
}
