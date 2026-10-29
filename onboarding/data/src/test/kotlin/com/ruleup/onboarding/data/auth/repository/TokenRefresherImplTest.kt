package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.token.TokenRepository
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.network.dto.ErrorBody
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.LogoutRequest
import com.ruleup.onboarding.data.auth.dto.SignUpRequest
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.TokenRefreshResponse
import com.ruleup.onboarding.data.auth.dto.WithdrawRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TokenRefresherImplTest {
    @Test
    fun `재발급에 성공하면 회전된 토큰을 반환한다`() =
        runBlocking {
            val api =
                FakeAuthApi {
                    BaseResponse(
                        success = true,
                        data =
                            TokenRefreshResponse(
                                accessToken = "new-access",
                                refreshToken = "new-refresh",
                                tokenType = "Bearer",
                                expiresIn = 1800,
                                userId = "u-1",
                            ),
                    )
                }

            val refreshed = refresher(api, "old-refresh").refresh("old-refresh")

            assertEquals("new-access", refreshed?.token?.accessToken)
            assertEquals("new-refresh", refreshed?.token?.refreshToken)
            assertEquals(1800, refreshed?.token?.expiresInSeconds)
            // 갱신만으로 세션이 완성되는지가 핵심이다
            assertEquals("u-1", refreshed?.userId)
        }

    @Test
    fun `HTTP 401 이면 세션 만료로 보고 null 을 반환한다`() =
        runBlocking {
            val api = FakeAuthApi { throw httpException(401) }

            assertNull(refresher(api, "expired-refresh").refresh("expired-refresh"))
        }

    @Test
    fun `SESSION_EXPIRED 에러 응답이면 null 을 반환한다`() =
        runBlocking {
            val api =
                FakeAuthApi {
                    BaseResponse(
                        success = false,
                        data = null,
                        error = ErrorBody(code = "SESSION_EXPIRED", message = "세션이 만료되었습니다."),
                    )
                }

            assertNull(refresher(api, "expired-refresh").refresh("expired-refresh"))
        }

    @Test
    fun `일시적 오류(5xx)는 세션을 유지하도록 예외를 전파한다`() =
        runBlocking {
            val api = FakeAuthApi { throw httpException(500) }

            assertFailsWith<HttpException> {
                refresher(api, "valid-refresh").refresh("valid-refresh")
            }
            Unit
        }

    @Test
    fun `같은 refreshToken 으로 동시에 갱신하면 요청은 한 번만 나가고 결과를 나눠 갖는다`() =
        runBlocking {
            val api =
                FakeAuthApi {
                    delay(100)
                    rotated()
                }
            val refresher = refresher(api)

            val results = listOf(async { refresher.refresh("r1") }, async { refresher.refresh("r1") }).awaitAll()

            assertEquals(1, api.refreshCalls)
            assertEquals(listOf("new-refresh", "new-refresh"), results.map { it?.token?.refreshToken })
        }

    @Test
    fun `갱신 응답을 기다리는 동안 로그아웃하면 응답이 세션을 복구하지 않는다`() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val api =
                FakeAuthApi {
                    started.complete(Unit)
                    release.await()
                    rotated()
                }
            val tokens = FakeTokenRepository(refreshToken = "r1")
            val refresher = TokenRefresherImpl(api, tokens)

            val pending = async { refresher.refresh("r1") }
            started.await()
            tokens.clear()
            release.complete(Unit)

            assertNull(pending.await())
            assertNull(tokens.getRefreshToken())
        }

    @Test
    fun `새 토큰 저장이 끝나기 전에는 갱신 결과를 반환하지 않는다`() =
        runBlocking {
            val saving = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val delegate = FakeTokenRepository(refreshToken = "r1")
            val tokens =
                object : TokenRepository by delegate {
                    override suspend fun saveTokensIfRefreshTokenMatches(
                        expectedRefreshToken: String,
                        token: Token,
                        userId: String?,
                    ): Boolean {
                        saving.complete(Unit)
                        release.await()
                        return delegate.saveTokensIfRefreshTokenMatches(expectedRefreshToken, token, userId)
                    }
                }
            val api = FakeAuthApi { rotated() }
            val refresher = TokenRefresherImpl(api, tokens)

            val first = async { refresher.refresh("r1") }
            saving.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) { refresher.refresh("r1") }
            assertFalse(first.isCompleted)
            assertFalse(second.isCompleted)
            release.complete(Unit)

            assertEquals("new-refresh", first.await()?.token?.refreshToken)
            assertEquals("new-refresh", second.await()?.token?.refreshToken)
            assertEquals("new-refresh", tokens.getRefreshToken())
            assertEquals(1, api.refreshCalls)
        }

    @Test
    fun `갱신을 기다리던 화면이 닫혀도 진행 중인 요청을 중복 전송하지 않는다`() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val api =
                FakeAuthApi {
                    started.complete(Unit)
                    release.await()
                    rotated()
                }
            val refresher = refresher(api)

            val first = async { refresher.refresh("r1") }
            started.await()
            first.cancelAndJoin()
            val second = async(start = CoroutineStart.UNDISPATCHED) { refresher.refresh("r1") }
            release.complete(Unit)

            assertEquals("new-refresh", second.await()?.token?.refreshToken)
            assertEquals(1, api.refreshCalls)
        }

    @Test
    fun `이미 끝난 갱신과 같은 토큰으로 늦게 와도 회전 전 토큰을 다시 보내지 않는다`() =
        runBlocking {
            val api = FakeAuthApi { rotated() }
            val refresher = refresher(api)

            refresher.refresh("r1")
            val late = refresher.refresh("r1")

            assertEquals(1, api.refreshCalls)
            assertNull(late)
        }

    @Test
    fun `두 번 회전한 뒤 지연된 이전 토큰으로 재발급하지 않는다`() =
        runBlocking {
            var rotation = 1
            val api =
                FakeAuthApi {
                    rotation++
                    rotated().let { it.copy(data = it.data?.copy(refreshToken = "r$rotation")) }
                }
            val tokens = FakeTokenRepository(refreshToken = "r1")
            val refresher = TokenRefresherImpl(api, tokens)

            refresher.refresh("r1")!!
            refresher.refresh("r2")!!
            val late = refresher.refresh("r1")

            assertEquals(2, api.refreshCalls)
            assertNull(late)
            assertEquals("r3", tokens.getRefreshToken())
        }

    @Test
    fun `일시적 오류 뒤 같은 토큰으로 다시 부르면 다시 요청한다`() =
        runBlocking {
            var fail = true
            val api =
                FakeAuthApi {
                    if (fail) {
                        fail = false
                        throw httpException(500)
                    }
                    rotated()
                }
            val refresher = refresher(api)

            assertFailsWith<HttpException> { refresher.refresh("r1") }
            val retried = refresher.refresh("r1")

            assertEquals(2, api.refreshCalls)
            assertEquals("new-refresh", retried?.token?.refreshToken)
        }

    private fun refresher(
        api: AuthApi,
        refreshToken: String = "r1",
    ) = TokenRefresherImpl(api, FakeTokenRepository(refreshToken = refreshToken))

    private fun rotated() =
        BaseResponse(
            success = true,
            data =
                TokenRefreshResponse(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    tokenType = "Bearer",
                    expiresIn = 1800,
                    userId = "u-1",
                ),
        )

    private fun httpException(code: Int): HttpException = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    private class FakeAuthApi(
        private val onRefresh: suspend () -> BaseResponse<TokenRefreshResponse>,
    ) : AuthApi {
        var refreshCalls = 0

        override suspend fun refreshToken(request: TokenRefreshRequest): BaseResponse<TokenRefreshResponse> {
            refreshCalls++
            return onRefresh()
        }

        override suspend fun socialLogin(
            provider: String,
            request: SocialLoginAuthRequest,
        ) = throw NotImplementedError()

        override suspend fun signup(request: SignUpRequest) = throw NotImplementedError()

        override suspend fun logout(request: LogoutRequest): BaseResponse<EmptyData> = throw NotImplementedError()

        override suspend fun withdraw(request: WithdrawRequest) = throw NotImplementedError()
    }
}
