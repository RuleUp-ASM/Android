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

    private fun httpException(code: Int): HttpException = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    private class FakeAuthApi(
        private val onRefresh: () -> BaseResponse<TokenRefreshResponse>,
    ) : AuthApi {
        override suspend fun refreshToken(request: TokenRefreshRequest): BaseResponse<TokenRefreshResponse> = onRefresh()

        override suspend fun socialLogin(
            provider: String,
            request: SocialLoginAuthRequest,
        ) = throw NotImplementedError()

        override suspend fun signup(request: SignUpRequest) = throw NotImplementedError()

        override suspend fun logout(request: LogoutRequest): BaseResponse<EmptyData> = throw NotImplementedError()
    }
}
