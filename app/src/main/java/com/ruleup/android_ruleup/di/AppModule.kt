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

/** 앱 전역 @Provides. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /** 도메인이 "오늘"을 읽는 통로. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    /** HTTP BODY 로깅은 디버그 빌드에서만 켠다(릴리스 로그 비용·토큰 유출 방지). */
    @Provides
    @Named(NetworkModule.DEBUG_LOGGING)
    fun provideNetworkDebugLogging(): Boolean = BuildConfig.DEBUG
}
