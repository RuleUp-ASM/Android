package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScreenAppSetTest {
    @Test
    fun `빈 선택으로는 만들 수 없다`() {
        assertFailsWith<InvalidScreenAppException> { ScreenAppSet.of(emptyList()) }
    }

    @Test
    fun `최대 개수를 넘으면 만들 수 없다`() {
        val tooMany = List(ScreenAppSet.MAX_COUNT + 1) { app("pkg$it") }

        assertFailsWith<InvalidScreenAppException> { ScreenAppSet.of(tooMany) }
    }

    @Test
    fun `packageName 이 겹치면 만들 수 없다`() {
        // 서버가 400 INVALID_APP 으로 돌려주는 조건이라 왕복 전에 끊는다.
        assertFailsWith<InvalidScreenAppException> {
            ScreenAppSet.of(listOf(app("com.a"), app("com.a")))
        }
    }

    @Test
    fun `1개부터 최대 개수까지는 통과한다`() {
        assertEquals(1, ScreenAppSet.of(listOf(app("com.a"))).apps.size)

        val max = List(ScreenAppSet.MAX_COUNT) { app("pkg$it") }
        assertEquals(ScreenAppSet.MAX_COUNT, ScreenAppSet.of(max).apps.size)
    }
}

private fun app(packageName: String) = ScreenApp(packageName = packageName, appName = packageName)
