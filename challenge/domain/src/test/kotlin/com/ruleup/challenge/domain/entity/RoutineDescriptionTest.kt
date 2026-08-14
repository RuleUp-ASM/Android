package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoutineDescriptionTest {
    @Test
    fun `앞뒤 공백은 털어서 담는다`() {
        assertEquals("아침 6시에 일어나고 싶어요", RoutineDescription.of("  아침 6시에 일어나고 싶어요  ").value)
    }

    @Test
    fun `빈 설명으로는 만들 수 없다`() {
        // 서버까지 보내지 않고 입력 화면에서 끊는다.
        assertFailsWith<IllegalArgumentException> { RoutineDescription.of("   ") }
    }

    @Test
    fun `최대 길이를 넘는 설명으로는 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            RoutineDescription.of("가".repeat(RoutineDescription.MAX_LENGTH + 1))
        }
    }

    @Test
    fun `최대 길이 정확히는 통과한다`() {
        val max = "가".repeat(RoutineDescription.MAX_LENGTH)

        assertEquals(max, RoutineDescription.of(max).value)
    }
}
