package com.ruleup.android_ruleup.di

import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.network.di.BaseUrl
import com.ruleup.network.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

/**
 * 앱 전역 @Provides. Retrofit base URL 을 local.properties → BuildConfig.BASE_URL 에서 공급한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /**
     * 도메인이 "오늘"을 읽는 통로.
     *
     * `LocalDate.now()` 를 직접 부르면 시간에 의존하는 규칙(만 14세 경계 등)을 테스트로 고정할 수
     * 없다. 만 나이는 사용자의 현지 날짜 기준이라 기기 시간대를 그대로 쓴다.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    /** HTTP BODY 로깅은 디버그 빌드에서만 켠다(릴리스 로그 비용·토큰 유출 방지). */
    @Provides
    @Named(NetworkModule.DEBUG_LOGGING)
    fun provideNetworkDebugLogging(): Boolean = BuildConfig.DEBUG
}
