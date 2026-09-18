package com.ruleup.profile.presentation.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 제재 해제 시각 표기. 날짜만 남기면 사용자가 "그날 언제"를 몰라 하루를 통째로 기다린다 —
 * 그래서 시각까지 붙이고, 못 읽는 문자열은 지어내지 않고 그대로 둔다.
 */
class SanctionUntilLabelTest {
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
        // 서버 표기가 바뀌어도 화면이 빈칸이 되지 않는다. 틀린 날짜를 만드는 것보다 낫다.
        assertEquals("곧 해제", sanctionUntilLabel("곧 해제"))
    }
}
