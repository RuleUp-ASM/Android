package com.ruleup.onboarding.domain.intro.usecase

import com.ruleup.onboarding.domain.entity.AppVersionGate
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheckAppVersionUseCaseTest {
    @Test
    fun `조회에 성공하면 버전 게이트를 그대로 반환한다`() =
        runBlocking {
            val gate =
                AppVersionGate(
                    forceUpdate = true,
                    devTestMsg = null,
                    minAppVersion = "1.0.0",
                    recommendAppVersion = "1.2.0",
                )
            val intro = FakeIntroRepository().apply { result = gate }

            val result = CheckAppVersionUseCase(intro)()

            assertEquals(gate, result)
        }

    @Test
    fun `조회가 실패하면 페일오픈으로 null 을 반환한다`() =
        runBlocking {
            val intro = FakeIntroRepository().apply { error = RuntimeException("network") }

            val result = CheckAppVersionUseCase(intro)()

            assertNull(result)
        }
}
