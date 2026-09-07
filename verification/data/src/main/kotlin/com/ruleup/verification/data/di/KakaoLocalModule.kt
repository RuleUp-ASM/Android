package com.ruleup.verification.data.di

import com.ruleup.verification.data.BuildConfig
import com.ruleup.verification.data.api.KakaoLocalApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Singleton

/**
 * 카카오 로컬(키워드 장소검색) 전용 Retrofit/OkHttp.
 *
 * 앱 백엔드와 base URL·인증이 달라(KakaoAK 헤더, dapi.kakao.com) 공용 OkHttpClient 를 재사용하지 않는다.
 * 공용 클라이언트는 모든 요청에 `Authorization: Bearer ...` 를 덮어써서, 그대로 쓰면 KakaoAK 헤더가 지워진다.
 */
@Module
@InstallIn(SingletonComponent::class)
object KakaoLocalModule {
    private const val KAKAO_LOCAL_BASE_URL = "https://dapi.kakao.com/"

    @Provides
    @Singleton
    fun provideKakaoLocalApi(json: Json): KakaoLocalApi {
        val authInterceptor =
            Interceptor { chain ->
                val request =
                    chain
                        .request()
                        .newBuilder()
                        .header("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
                        .build()
                chain.proceed(request)
            }
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(authInterceptor)
                .build()
        return Retrofit
            .Builder()
            .baseUrl(KAKAO_LOCAL_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }
}
