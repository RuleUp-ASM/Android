package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import kotlinx.coroutines.runBlocking
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoLoginUseCaseTest {
    @Test
    fun `refreshToken 이 없으면 재발급을 시도하지 않는다`() =
        runBlocking {
            val auth = FakeAuthRepository()
            val tokens = FakeTokenRepository(refreshToken = null)

            val result = AutoLoginUseCase(auth, tokens, RecordingBizLogger())()

            assertEquals(AutoLoginResult.NoSession, result)
            assertNull(auth.refreshCalledWith)
            assertFalse(tokens.cleared)
            assertNull(tokens.savedToken)
        }

    @Test
    fun `재발급에 성공하면 새 토큰을 저장한다`() =
        runBlocking {
            val newToken = Token("a", "r2", "Bearer", 3600)
            val auth = FakeAuthRepository().apply { refreshResult = RefreshedSession(newToken, userId = "u-1") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens, RecordingBizLogger())()

            assertEquals(AutoLoginResult.Authenticated, result)
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

            AutoLoginUseCase(auth, tokens, RecordingBizLogger())()

            assertEquals("u-old", tokens.savedUserId)
        }

    @Test
    fun `연결이 끊겨 재발급하지 못하면 토큰을 남겨 다음 실행에 다시 시도한다`() =
        runBlocking {
            // 전송 실패는 세션이 끝난 게 아니다. 여기서 지우면 지하철에서 앱을 한 번 연 것만으로
            // 며칠 남은 refreshToken 이 사라지고 소셜 로그인부터 다시 해야 한다.
            val auth = FakeAuthRepository().apply { refreshError = SocketTimeoutException("timeout") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens, RecordingBizLogger())()

            // 세션 만료와 같은 값으로 접으면 진입 화면이 둘을 구분하지 못해 타임아웃 한 번에
            // 로그인 화면으로 떨어진다(ENV-03).
            assertEquals(AutoLoginResult.ConnectionFailed, result)
            assertFalse(tokens.cleared)
        }

    @Test
    fun `재발급에 실패하면 로컬 토큰을 정리한다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { refreshError = RuntimeException("expired") }
            val tokens = FakeTokenRepository(refreshToken = "r1")

            val result = AutoLoginUseCase(auth, tokens, RecordingBizLogger())()

            assertEquals(AutoLoginResult.SessionExpired, result)
            assertTrue(tokens.cleared)
            assertNull(tokens.savedToken)
        }
}
