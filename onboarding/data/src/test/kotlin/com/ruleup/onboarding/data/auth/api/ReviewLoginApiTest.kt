package com.ruleup.onboarding.data.auth.api

import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.token.TokenRefresher
import com.ruleup.network.auth.TokenAuthenticator
import com.ruleup.network.di.NetworkModule
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.data.auth.dto.ReviewLoginRequest
import com.ruleup.onboarding.data.auth.repository.AuthRepositoryImpl
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import com.ruleup.onboarding.domain.auth.repository.SignupInviteStore
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewLoginApiTest {
    @Test
    fun `잘못된 코드와 횟수 제한 및 비활성화 오류를 구분한다`() =
        runBlocking {
            val tokens = FakeTokenRepository()
            val observability = testObservability()
            val authenticator = TokenAuthenticator(tokens, Lazy { error("갱신하면 안 됨") }, observability)
            var status = 400
            var responseCode = "INVALID_REVIEW_CODE"
            val client =
                NetworkModule
                    .provideOkHttpClient(tokens, authenticator, false, observability)
                    .newBuilder()
                    .addInterceptor { chain ->
                        val body = """{"success":false,"error":{"code":"$responseCode","message":"error"}}"""
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(status)
                            .message("error")
                            .body(body.toResponseBody("application/json".toMediaType()))
                            .build()
                    }.build()
            val api =
                NetworkModule
                    .provideAuthRetrofit(client, NetworkModule.provideJson(), "https://example.invalid/api/")
                    .create(AuthApi::class.java)
            val repository =
                AuthRepositoryImpl(
                    api = api,
                    deviceInfoProvider = DeviceInfoProvider { error("기기 정보 불필요") },
                    tokenRefresher =
                        object : TokenRefresher {
                            override suspend fun refresh(refreshToken: String) = error("갱신 불필요")
                        },
                    signupInviteStore =
                        object : SignupInviteStore {
                            override fun capture(link: String) = Unit

                            override suspend fun currentLink(): String? = null

                            override fun clear() = Unit
                        },
                )

            assertEquals(AuthFailure.INVALID_REVIEW_CODE, assertFailsWith<AuthException> { repository.reviewLogin("bad") }.failure)
            status = 429
            responseCode = "TOO_MANY_ATTEMPTS"
            assertEquals(AuthFailure.REVIEW_TOO_MANY_ATTEMPTS, assertFailsWith<AuthException> { repository.reviewLogin("bad") }.failure)
            status = 404
            responseCode = "NOT_FOUND"
            assertEquals(AuthFailure.REVIEW_UNAVAILABLE, assertFailsWith<AuthException> { repository.reviewLogin("bad") }.failure)
        }

    @Test
    fun `심사 로그인 401은 기존 계정의 토큰으로 재시도하지 않는다`() =
        runBlocking {
            val tokens = FakeTokenRepository()
            tokens.saveSession(Token("old-access", "old-refresh", "Bearer", 3600), "old-user")
            val authenticator = TokenAuthenticator(tokens, Lazy { error("갱신하면 안 됨") }, testObservability())
            val request = Request.Builder().url("https://example.invalid/api/v1/auth/review-login").build()
            val response =
                Response
                    .Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .build()

            assertNull(authenticator.authenticate(null, response))
        }

    @Test
    fun `심사 로그인은 기존 토큰을 붙이지 않고 코드만 보낸다`() =
        runBlocking {
            val tokens = FakeTokenRepository()
            tokens.saveSession(Token("old-access", "old-refresh", "Bearer", 3600), "old-user")
            val sink = RecordingSink()
            val observability = testObservability(sink)
            val authenticator = TokenAuthenticator(tokens, Lazy { error("인증 갱신 불필요") }, observability)
            var actualPath: String? = null
            var authorization: String? = null
            var body: String? = null
            val client =
                NetworkModule
                    .provideOkHttpClient(tokens, authenticator, true, observability)
                    .newBuilder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        actualPath = request.url.encodedPath
                        authorization = request.header("Authorization")
                        body = request.body?.let { requestBody -> Buffer().also { requestBody.writeTo(it) }.readUtf8() }
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(
                                """{"success":true,"data":{"isNewUser":false,"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":3600,"user":{"id":"review-user","nickname":"심사"}}}"""
                                    .toResponseBody(
                                        "application/json".toMediaType(),
                                    ),
                            ).build()
                    }.build()
            val api =
                NetworkModule
                    .provideAuthRetrofit(client, NetworkModule.provideJson(), "https://example.invalid/api/")
                    .create(AuthApi::class.java)

            val response = api.reviewLogin(ReviewLoginRequest("TEST-CODE"))

            assertEquals("/api/v1/auth/review-login", actualPath)
            assertNull(authorization)
            assertEquals("""{"code":"TEST-CODE"}""", body)
            assertEquals("new-access", response.data?.accessToken)
            val messages = sink.payloads.filterIsInstance<DiagnosticPayload>().map { it.message }
            assertTrue(messages.none { "TEST-CODE" in it || "new-access" in it || "new-refresh" in it })
            client.newCall(Request.Builder().url("https://example.invalid/api/v1/profile").build()).execute().close()
            assertTrue(sink.payloads.filterIsInstance<DiagnosticPayload>().any { it.tag == "HttpClient" })
        }
}
