package com.ruleup.network.auth

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.domain.token.TokenRefresher
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.test.testObservability
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenAuthenticatorTest {
    @Test
    fun `리프레시 토큰 조회 중 다른 경로가 갱신하면 새 토큰으로 재시도한다`() =
        runBlocking {
            val stored = FakeTokenRepository()
            stored.saveSession(Token("a1", "r1", "Bearer", 3600), "u1")
            val tokens =
                object : TokenRepository by stored {
                    override suspend fun getRefreshToken(): String? {
                        stored.saveTokens(Token("a2", "r2", "Bearer", 3600), "u1")
                        return stored.getRefreshToken()
                    }
                }
            val refresher =
                object : TokenRefresher {
                    override suspend fun refresh(refreshToken: String): RefreshedSession? = error("중복 갱신")
                }
            val response =
                Response
                    .Builder()
                    .request(
                        Request
                            .Builder()
                            .url("https://example.com/api/v1/profile")
                            .header("Authorization", "Bearer a1")
                            .build(),
                    ).protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .build()

            val retried = TokenAuthenticator(tokens, Lazy { refresher }, testObservability()).authenticate(null, response)

            assertEquals("Bearer a2", retried?.header("Authorization"))
        }

    @Test
    fun `동시에 만료된 요청들은 한 번만 갱신하고 같은 새 토큰을 사용한다`() =
        runBlocking {
            val tokens = FakeTokenRepository()
            tokens.saveSession(Token("a1", "r1", "Bearer", 3600), "u1")
            val calls = AtomicInteger()
            val refresher =
                object : TokenRefresher {
                    override suspend fun refresh(refreshToken: String): RefreshedSession {
                        calls.incrementAndGet()
                        val session = RefreshedSession(Token("a2", "r2", "Bearer", 3600), "u1")
                        tokens.saveTokensIfRefreshTokenMatches(refreshToken, session.token, session.userId)
                        return session
                    }
                }
            val authenticator = TokenAuthenticator(tokens, Lazy { refresher }, testObservability())
            val request =
                Request
                    .Builder()
                    .url("https://example.com/api/v1/profile")
                    .header("Authorization", "Bearer a1")
                    .build()
            val response =
                Response
                    .Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .build()
            val executor = Executors.newFixedThreadPool(8)
            val ready = CountDownLatch(8)
            val start = CountDownLatch(1)
            try {
                val results =
                    List(8) {
                        executor.submit(
                            Callable {
                                ready.countDown()
                                check(start.await(10, TimeUnit.SECONDS))
                                authenticator.authenticate(null, response)?.header("Authorization")
                            },
                        )
                    }
                assertTrue(ready.await(10, TimeUnit.SECONDS))
                start.countDown()
                assertEquals(List(8) { "Bearer a2" }, results.map { it.get(10, TimeUnit.SECONDS) })
                assertEquals(1, calls.get())
                assertEquals("r2", tokens.getRefreshToken())
            } finally {
                start.countDown()
                executor.shutdownNow()
            }
        }
}
