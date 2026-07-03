package com.ruleup.network.di

import com.ruleup.domain.token.TokenRepository
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
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** app 계층이 BuildConfig.DEBUG 로 채워 주입하는 HTTP 로깅 on/off 플래그. */
    const val DEBUG_LOGGING = "network_debug_logging"

    // Authorization 헤더를 붙이면 안 되는 공개(비인증) 엔드포인트 경로 조각(명세 /auth/*).
    // 로그아웃은 액세스 토큰이 필요하므로 제외한다.
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
        @Named(DEBUG_LOGGING) debugLogging: Boolean,
    ): OkHttpClient {
        // 저장된 accessToken 이 있으면 매 요청마다 Authorization 헤더로 주입한다.
        // 단, 로그인/가입/토큰갱신 같은 공개(비인증) 엔드포인트에는 헤더를 붙이지 않는다.
        // (만료된 토큰이 로그인 요청에 실려 나가면 백엔드 JWT 필터가 401 로 막아버린다.)
        val authInterceptor =
            Interceptor { chain ->
                val original = chain.request()
                val skipAuth = NO_AUTH_PATHS.any { original.url.encodedPath.contains(it) }
                // 캐시 스냅샷을 먼저 읽어 워커 스레드 블로킹을 피하고, 콜드(앱 재시작 직후 첫 요청)일 때만 1회 블로킹 조회한다.
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

        // BODY 로깅은 디버그 빌드에서만 장착한다. 릴리스에선 전체 본문 버퍼링·문자열화 비용과
        // 토큰/좌표·헬스 페이로드 유출 위험을 모두 제거한다. 디버그에서도 인증 헤더는 마스킹.
        if (debugLogging) {
            val loggingInterceptor =
                HttpLoggingInterceptor { message -> Timber.tag("HttpClient").d(message) }
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
        // Retrofit 은 baseUrl 이 trailing slash 로 끝나길 강제한다. local.properties 의 BASE_URL 에
        // 슬래시를 빠뜨려도 앱이 시작부터 죽지 않도록, 비어있지 않으면 보정한다(빈 값은 그대로 fail-fast).
        val normalized = if (baseUrl.isNotBlank() && !baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        return Retrofit
            .Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
