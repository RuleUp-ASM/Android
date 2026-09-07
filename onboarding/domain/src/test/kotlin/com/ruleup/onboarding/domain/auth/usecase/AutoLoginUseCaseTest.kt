package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.observability.domain.test.testObservability
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

            val result = AutoLoginUseCase(auth, tokens, testObservability())()

            assertFalse(result)
            assertNull(auth.refreshCalledWith)
            assertFalse(tokens.cleared)
            assertNull(tokens.savedToken)
        }

    @Test
    fun `재발급에 성공하면 새 토큰을 저장하고 true 를 반환한다`() =
        runBlocking {
            val newToken = Token("a", "r2", "Bearer", 3600)
            val auth = FakeAuthRepository().apply { refreshResult = RefreshedSession(newToken, userId = "u-1") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens, testObservability())()

            assertTrue(result)
            assertEquals(newToken, tokens.savedToken)
            // 갱신 응답의 userId 로 세션이 완성된다. 비면 사용자 귀속이 끊긴 채 홈에 들어간다.
            assertEquals("u-1", tokens.savedUserId)
            assertFalse(tokens.cleared)
        }

    @Test
    fun `갱신 응답에 userId 가 없으면 기존 값을 유지한다`() =
        runBlocking {
            // 이 필드를 안 내려주는 서버 배포본. 덮어 비우면 사용자 귀속이 끊긴다.
            val auth =
                FakeAuthRepository().apply {
                    refreshResult = RefreshedSession(Token("a", "r2", "Bearer", 3600), userId = null)
                }
            val tokens = FakeTokenRepository(refreshToken = "r1").apply { savedUserId = "u-old" }

            AutoLoginUseCase(auth, tokens, testObservability())()

            assertEquals("u-old", tokens.savedUserId)
        }

    @Test
    fun `재발급에 실패하면 로컬 토큰을 정리하고 false 를 반환한다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { refreshError = RuntimeException("expired") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens, testObservability())()

            assertFalse(result)
            assertTrue(tokens.cleared)
            assertNull(tokens.savedToken)
        }
}
