package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.helper.LocalUserDataCleaner
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

            LogoutUseCase(auth, tokens, RecordingCleaner())()

            assertEquals("r1", auth.loggedOutWith)
            assertTrue(tokens.cleared)
        }

    @Test
    fun `서버 revoke 가 실패해도 로컬 토큰은 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { logoutError = RuntimeException("server down") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            LogoutUseCase(auth, tokens, RecordingCleaner())()

            assertTrue(tokens.cleared)
        }

    @Test
    fun `refreshToken 이 없으면 서버 revoke 없이 로컬만 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = null)

            LogoutUseCase(auth, tokens, RecordingCleaner())()

            assertNull(auth.loggedOutWith)
            assertTrue(tokens.cleared)
        }

    @Test
    fun `토큰뿐 아니라 단말에 남은 수집 데이터도 지운다`() =
        runBlocking {
            // 토큰만 지우면 OS 지오펜스와 수집 버퍼가 남아 다음 사용자가 앞 사용자의 신호를
            // 자기 것으로 올린다(AUTH-10).
            val cleaner = RecordingCleaner()

            LogoutUseCase(FakeAuthRepository(), FakeTokenRepository(refreshToken = "r1"), cleaner)()

            assertTrue(cleaner.called)
        }

    private class RecordingCleaner : LocalUserDataCleaner {
        var called = false
            private set

        override suspend fun clear() {
            called = true
        }
    }
}
