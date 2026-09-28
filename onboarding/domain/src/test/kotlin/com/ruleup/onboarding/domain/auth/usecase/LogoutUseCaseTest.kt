package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.helper.LocalUserDataCleaner
import com.ruleup.domain.helper.PushTokenRevoker
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

            LogoutUseCase(auth, tokens, RecordingCleaner(), noopRevoker)()

            assertEquals("r1", auth.loggedOutWith)
            assertTrue(tokens.cleared)
        }

    @Test
    fun `서버 revoke 가 실패해도 로컬 토큰은 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { logoutError = RuntimeException("server down") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            LogoutUseCase(auth, tokens, RecordingCleaner(), noopRevoker)()

            assertTrue(tokens.cleared)
        }

    @Test
    fun `refreshToken 이 없으면 서버 revoke 없이 로컬만 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = null)

            LogoutUseCase(auth, tokens, RecordingCleaner(), noopRevoker)()

            assertNull(auth.loggedOutWith)
            assertTrue(tokens.cleared)
        }

    @Test
    fun `토큰뿐 아니라 단말에 남은 수집 데이터도 지운다`() =
        runBlocking {
            val cleaner = RecordingCleaner()

            LogoutUseCase(FakeAuthRepository(), FakeTokenRepository(refreshToken = "r1"), cleaner, noopRevoker)()

            assertTrue(cleaner.called)
        }

    @Test
    fun `푸시 토큰은 세션 revoke 와 로컬 정리보다 먼저 해제한다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = "r1")
            var revokedWhileSessionAlive = false
            val revoker =
                PushTokenRevoker {
                    revokedWhileSessionAlive = auth.loggedOutWith == null && !tokens.cleared
                }

            LogoutUseCase(auth, tokens, RecordingCleaner(), revoker)()

            assertTrue(revokedWhileSessionAlive)
        }

    @Test
    fun `refreshToken 이 없어도 푸시 토큰은 해제한다`() =
        runBlocking {
            var revoked = false

            LogoutUseCase(FakeAuthRepository(), FakeTokenRepository(refreshToken = null), RecordingCleaner()) {
                revoked = true
            }()

            assertTrue(revoked)
        }

    private val noopRevoker = PushTokenRevoker {}

    private class RecordingCleaner : LocalUserDataCleaner {
        var called = false
            private set

        override suspend fun clear() {
            called = true
        }
    }
}
