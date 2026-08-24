package com.ruleup.challenge.presentation.targets

import com.ruleup.verification.domain.entity.InvalidScreenAppException
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TargetAppsCopyTest {
    @Test
    fun `대상 앱은 1에서 10개만 허용된다`() {
        // 11개 이상은 서버가 400 INVALID_APP 으로 막는다 — 왕복 전에 값 타입이 먼저 잠근다.
        assertFailsWith<InvalidScreenAppException> { ScreenAppSet.of(apps(11)) }
        assertEquals(10, ScreenAppSet.of(apps(10)).apps.size)
    }

    @Test
    fun `같은 패키지를 두 번 담을 수 없다`() {
        val duplicated = listOf(app("com.a"), app("com.a"))

        assertFailsWith<InvalidScreenAppException> { ScreenAppSet.of(duplicated) }
    }

    private fun apps(count: Int): List<ScreenApp> = (1..count).map { app("com.app$it") }

    private fun app(packageName: String): ScreenApp = ScreenApp(packageName = packageName, appName = packageName)
}
