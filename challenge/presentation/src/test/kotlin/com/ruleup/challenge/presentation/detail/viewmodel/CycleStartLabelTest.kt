package com.ruleup.challenge.presentation.detail.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

/** 주 중간 참여 안내 토스트. */
class CycleStartLabelTest {
    @Test
    fun `판정 시작일은 월과 일로 읽히게 바꾼다`() {
        assertEquals("9월 21일", cycleStartLabel("2026-09-21"))
    }

    @Test
    fun `날짜 형식이 아니면 받은 값을 지어내지 않고 그대로 쓴다`() {
        assertEquals("2026-W39", cycleStartLabel("2026-W39"))
    }
}
