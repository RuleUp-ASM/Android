package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.entity.user.Token
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoLoginUseCaseTest {
    @Test
    fun `refreshToken 이 없으면 재발급을 시도하지 않고 false 를 반환한다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = null)

            val result = AutoLoginUseCase(auth, tokens)()

            assertFalse(result)
            assertNull(auth.refreshCalledWith)
            assertFalse(tokens.cleared)
            assertNull(tokens.savedToken)
        }

    @Test
    fun `재발급에 성공하면 새 토큰을 저장하고 true 를 반환한다`() =
        runBlocking {
            val newToken = Token("a", "r2", "Bearer", 3600)
            val auth = FakeAuthRepository().apply { refreshResult = newToken }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens)()

            assertTrue(result)
            assertEquals(newToken, tokens.savedToken)
            assertFalse(tokens.cleared)
        }

    @Test
    fun `재발급에 실패하면 로컬 토큰을 정리하고 false 를 반환한다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { refreshError = RuntimeException("expired") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens)()

            assertFalse(result)
            assertTrue(tokens.cleared)
            assertNull(tokens.savedToken)
        }
}
