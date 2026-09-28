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
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** app 계층이 BuildConfig.DEBUG 로 채워 주입하는 HTTP 로깅 on/off 플래그. */
    const val DEBUG_LOGGING = "network_debug_logging"

    /** 인증 API(/auth 경로) 전용 Retrofit. */
    const val AUTH_RETROFIT = "auth_retrofit"

    // 명세 /auth/* 중 헤더를 붙이면 안 되는 비인증 엔드포인트.
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
                // 캐시가 비는 건 앱 재시작 직후 첫 요청뿐
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
                // 기본값(10초)에 기대면 값이 코드에 안 보여 조정할 수도, 근거를 댈 수도 없다.
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                // 4xx 업무 오류 본문을 Retrofit 이 읽게 한다(#417).
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

    /** 토큰 갱신은 별도 [Dispatcher] 로 보낸다. */
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
        // Retrofit 은 trailing slash 를 강제한다
        val normalized = if (baseUrl.isNotBlank() && !baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        return Retrofit
            .Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
