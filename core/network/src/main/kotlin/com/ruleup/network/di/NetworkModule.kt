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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
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
    fun provideOkHttpClient(tokenRepository: TokenRepository): OkHttpClient {
        // 저장된 accessToken 이 있으면 매 요청마다 Authorization 헤더로 주입한다.
        // 토큰이 없는 로그인/가입 등 비인증 요청에는 헤더를 붙이지 않는다.
        val authInterceptor =
            Interceptor { chain ->
                val token = runBlocking { tokenRepository.getAccessToken() }
                val request =
                    if (!token.isNullOrBlank()) {
                        chain
                            .request()
                            .newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    } else {
                        chain.request()
                    }
                chain.proceed(request)
            }

        // 기본 로깅 인터셉터를 Timber 로 출력해 Logcat 에서 HttpClient 태그로 보이게 한다.
        val loggingInterceptor =
            HttpLoggingInterceptor { message -> Timber.tag("HttpClient").d(message) }
                .apply { level = HttpLoggingInterceptor.Level.BODY }

        return OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
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
