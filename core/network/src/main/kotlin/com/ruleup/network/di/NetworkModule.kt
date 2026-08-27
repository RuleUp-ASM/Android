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
    ): Retrofit {
        // Retrofit 은 trailing slash 를 강제한다 — local.properties 의 BASE_URL 에서 빠뜨려도 죽지 않게 보정한다.
        // 빈 값은 보정하지 않고 그대로 던져 설정 누락을 바로 드러낸다.
        val normalized = if (baseUrl.isNotBlank() && !baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        return Retrofit
            .Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
