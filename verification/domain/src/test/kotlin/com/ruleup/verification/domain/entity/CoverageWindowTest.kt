package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** sync 구간 선언. 끝이 시작보다 앞선 구간은 서버가 400 으로 막으므로 만들어지는 순간 막는다. */
class CoverageWindowTest {
    @Test
    fun `구간 끝이 시작보다 앞서면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { CoverageWindow(from = 200L, until = 100L) }
    }

    @Test
    fun `빈 구간은 시작 시각에 길이 0 으로 선다`() {
        assertEquals(CoverageWindow(100L, 100L), CoverageWindow(100L, 200L).emptyAtStart())
    }
}
