package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogoutUseCaseTest {
    @Test
    fun `refreshToken 이 있으면 서버 revoke 후 로컬 토큰을 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = "r1")

            LogoutUseCase(auth, tokens)()

            assertEquals("r1", auth.loggedOutWith)
            assertTrue(tokens.cleared)
        }

    @Test
    fun `서버 revoke 가 실패해도 로컬 토큰은 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { logoutError = RuntimeException("server down") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            LogoutUseCase(auth, tokens)()

            assertTrue(tokens.cleared)
        }

    @Test
    fun `refreshToken 이 없으면 서버 revoke 없이 로컬만 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = null)

            LogoutUseCase(auth, tokens)()

            assertNull(auth.loggedOutWith)
            assertTrue(tokens.cleared)
        }
}
