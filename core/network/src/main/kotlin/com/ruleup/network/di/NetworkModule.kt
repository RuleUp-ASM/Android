package com.ruleup.network.di

import com.ruleup.domain.token.TokenRepository
import com.ruleup.network.auth.TokenAuthenticator
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.d
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** app 계층이 BuildConfig.DEBUG 로 채워 주입하는 HTTP 로깅 on/off 플래그. */
    const val DEBUG_LOGGING = "network_debug_logging"

    /** 인증 API(/auth 경로) 전용 Retrofit. 토큰 갱신이 일반 요청과 디스패처를 나눠 쓰게 한다. */
    const val AUTH_RETROFIT = "auth_retrofit"

    // 명세 /auth/* 중 헤더를 붙이면 안 되는 비인증 엔드포인트.
    // 로그아웃은 액세스 토큰이 필요하므로 여기 넣지 않는다.
    private val NO_AUTH_PATHS =
        listOf(
            "/auth/oauth",
            "/auth/signup",
            "/auth/refresh",
        )

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenRepository: TokenRepository,
        tokenAuthenticator: TokenAuthenticator,
        @Named(DEBUG_LOGGING) debugLogging: Boolean,
        observability: Observability,
    ): OkHttpClient {
        // 만료된 토큰이 NO_AUTH_PATHS 요청에 실려 나가면 백엔드 JWT 필터가 401 로 막아버린다.
        val authInterceptor =
            Interceptor { chain ->
                val original = chain.request()
                val skipAuth = NO_AUTH_PATHS.any { original.url.encodedPath.contains(it) }
                // 캐시가 비는 건 앱 재시작 직후 첫 요청뿐 — 그때만 한 번 블로킹한다.
                val token = tokenRepository.cachedAccessToken() ?: runBlocking { tokenRepository.getAccessToken() }
                val request =
                    if (!token.isNullOrBlank() && !skipAuth) {
                        original
                            .newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    } else {
                        original
                    }
                chain.proceed(request)
            }

        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(authInterceptor)
                // 4xx 업무 오류 본문을 Retrofit 이 읽게 한다(#417). authenticator 뒤에 오는 최종
                // 응답만 보므로 401 재발급 경로는 건드리지 않는다.
                .addInterceptor(ErrorBodyInterceptor())
                .authenticator(tokenAuthenticator)

        if (debugLogging) {
            val loggingInterceptor =
                HttpLoggingInterceptor { message -> observability.d("HttpClient") { message } }
                    .apply {
                        level = HttpLoggingInterceptor.Level.BODY
                        redactHeader("Authorization")
                        redactHeader("Cookie")
                    }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @BaseUrl baseUrl: String,
    ): Retrofit = buildRetrofit(okHttpClient, json, baseUrl)

    /**
     * 토큰 갱신은 별도 [Dispatcher] 로 보낸다. 만료 토큰으로 동시에 401 을 받은 호출들은 Authenticator 안에서
     * 갱신을 기다리며 기본 디스패처의 호스트당 동시 한도(5)를 붙잡는다 — 갱신까지 같은 디스패처에 줄 서면
     * 영영 출발하지 못하고 앱 전체 요청이 멈춘다.
     */
    @Provides
    @Singleton
    @Named(AUTH_RETROFIT)
    fun provideAuthRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @BaseUrl baseUrl: String,
    ): Retrofit = buildRetrofit(okHttpClient.newBuilder().dispatcher(Dispatcher()).build(), json, baseUrl)

    private fun buildRetrofit(
        client: OkHttpClient,
        json: Json,
        baseUrl: String,
    ): Retrofit {
        // Retrofit 은 trailing slash 를 강제한다 — local.properties 의 BASE_URL 에서 빠뜨려도 죽지 않게 보정한다.
        // 빈 값은 보정하지 않고 그대로 던져 설정 누락을 바로 드러낸다.
        val normalized = if (baseUrl.isNotBlank() && !baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        return Retrofit
            .Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
