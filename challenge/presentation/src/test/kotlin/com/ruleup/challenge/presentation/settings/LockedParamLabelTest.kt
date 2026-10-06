package com.ruleup.challenge.presentation.settings

import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import kotlin.test.Test
import kotlin.test.assertEquals

/** 잠긴 목표 표기(#593). 서버 key · unit 을 그대로 쓰면 「duration min · 30min」이 사용자에게 보인다. */
class LockedParamLabelTest {
    @Test
    fun `잠긴 숫자 목표는 편집기와 같은 이름과 한글 단위로 보인다`() {
        assertEquals("목표값 · 30분", spec(key = "duration_min", value = "30", kind = ParamKind.NUMBER, unit = "min").lockedLabel())
    }

    @Test
    fun `잠긴 시각 목표는 시각만 보인다`() {
        assertEquals("목표 시각 · 06:30", spec(key = "wake_time", value = "06:30", kind = ParamKind.TIME, unit = null).lockedLabel())
    }

    private fun spec(
        key: String,
        value: String,
        kind: ParamKind,
        unit: String?,
    ) = ParamSpec(key = key, value = value, defaultValue = value, kind = kind, unit = unit, min = null, max = null)
}
