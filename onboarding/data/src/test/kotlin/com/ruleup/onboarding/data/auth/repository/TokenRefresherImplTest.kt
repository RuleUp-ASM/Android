package com.ruleup.onboarding.data.auth.repository

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

            val refreshed = TokenRefresherImpl(api).refresh("old-refresh")

            assertEquals("new-access", refreshed?.token?.accessToken)
            assertEquals("new-refresh", refreshed?.token?.refreshToken)
            assertEquals(1800, refreshed?.token?.expiresInSeconds)
            // 갱신만으로 세션이 완성되는지가 핵심이다 — 이 값이 비면 별도 조회가 되살아난다.
            assertEquals("u-1", refreshed?.userId)
        }

    @Test
    fun `HTTP 401 이면 세션 만료로 보고 null 을 반환한다`() =
        runBlocking {
            val api = FakeAuthApi { throw httpException(401) }

            assertNull(TokenRefresherImpl(api).refresh("expired-refresh"))
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

            assertNull(TokenRefresherImpl(api).refresh("expired-refresh"))
        }

    @Test
    fun `일시적 오류(5xx)는 세션을 유지하도록 예외를 전파한다`() =
        runBlocking {
            val api = FakeAuthApi { throw httpException(500) }

            assertFailsWith<HttpException> {
                TokenRefresherImpl(api).refresh("valid-refresh")
            }
            Unit
        }

    @Test
    fun `같은 refreshToken 으로 동시에 갱신하면 요청은 한 번만 나가고 결과를 나눠 갖는다`() =
        runBlocking {
            // 두 번 나가면 서버가 첫 요청에서 토큰을 회전해 두 번째가 401 을 받고, 그쪽이 세션을 지운다(AUTH-09).
            val api =
                FakeAuthApi {
                    delay(100)
                    rotated()
                }
            val refresher = TokenRefresherImpl(api)

            val results = listOf(async { refresher.refresh("r1") }, async { refresher.refresh("r1") }).awaitAll()

            assertEquals(1, api.refreshCalls)
            assertEquals(listOf("new-refresh", "new-refresh"), results.map { it?.token?.refreshToken })
        }

    @Test
    fun `이미 끝난 갱신과 같은 토큰으로 늦게 와도 회전 전 토큰을 다시 보내지 않는다`() =
        runBlocking {
            val api = FakeAuthApi { rotated() }
            val refresher = TokenRefresherImpl(api)

            refresher.refresh("r1")
            val late = refresher.refresh("r1")

            assertEquals(1, api.refreshCalls)
            assertEquals("new-refresh", late?.token?.refreshToken)
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
            val refresher = TokenRefresherImpl(api)

            assertFailsWith<HttpException> { refresher.refresh("r1") }
            val retried = refresher.refresh("r1")

            assertEquals(2, api.refreshCalls)
            assertEquals("new-refresh", retried?.token?.refreshToken)
        }

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
