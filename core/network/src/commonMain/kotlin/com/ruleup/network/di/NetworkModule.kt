package com.ruleup.network.di

import com.ruleup.domain.token.TokenRepository
import de.jensklingenberg.ktorfit.Ktorfit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
interface NetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(
        json: Json,
        tokenRepository: TokenRepository,
    ): HttpClient =
        HttpClient {
            // BaseResponse 로 에러를 표현하므로 비-2xx 응답에서 예외를 던지지 않는다.
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }
            // Ktorfit 생성 코드는 @Body 를 setBody(request) 로만 넘기고 Content-Type 을 설정하지 않는다.
            // Content-Type 이 없으면 ContentNegotiation 이 요청 body 직렬화를 건너뛰어 기본 변환기로 떨어지고
            // "Fail to prepare request body … Content-Type: null" 로 모든 POST(소셜 로그인 포함)가 실패한다.
            // 모든 요청 기본 Content-Type 을 JSON 으로 지정해 직렬화가 적용되게 한다.
            install(DefaultRequest) {
                contentType(ContentType.Application.Json)
            }
            // 기본 Logger 는 Android 에서 SLF4J(바인딩 없으면 NOP)로 떨어져 Logcat 에 안 찍힌다.
            // Kermit 로 직접 출력해 Android Logcat / iOS os_log 양쪽에서 보이게 한다.
            install(Logging) {
                logger =
                    object : Logger {
                        private val kermit = KermitLogger.withTag("HttpClient")

                        override fun log(message: String) {
                            kermit.d { message }
                        }
                    }
                level = LogLevel.BODY
            }
            // 저장된 accessToken 이 있으면 매 요청마다 Authorization 헤더로 주입한다.
            // 토큰이 없는 로그인/가입 등 비인증 요청에는 헤더를 붙이지 않는다.
            install(
                createClientPlugin("AuthHeaderPlugin") {
                    onRequest { request, _ ->
                        val token = tokenRepository.getAccessToken()
                        if (!token.isNullOrBlank()) {
                            request.header(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                },
            )
        }

    // Ktorfit 인스턴스. 공유 HttpClient(인증/로깅/직렬화 플러그인 포함)를 그대로 쓰고,
    // 각 data 모듈의 API 인터페이스는 ktorfit.createXxxApi() 로 생성한다.
    // baseUrl 은 trailing slash 로 끝나야 하며 인터페이스의 경로는 leading slash 없이 상대 경로로 쓴다.
    @Provides
    @SingleIn(AppScope::class)
    fun provideKtorfit(
        httpClient: HttpClient,
        @BaseUrl baseUrl: String,
    ): Ktorfit =
        Ktorfit
            .Builder()
            .httpClient(httpClient)
            .baseUrl(baseUrl)
            .build()
}
