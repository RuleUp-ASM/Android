package com.ruleup.android_ruleup.di

import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.observability.domain.model.AmplitudeApiKey
import com.ruleup.observability.domain.model.BuildProfile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 관측 파이프라인이 요구하는 앱 단위 값.
 *
 * [BuildProfile] 은 `:observability:data` 가 제공할 수 없다 — 라이브러리 모듈은 앱의
 * `BuildConfig` 를 못 보고, 자체 `BuildConfig.DEBUG` 는 빌드 타입만 알기 때문이다.
 *
 * 현재 buildTypes 는 `debug`/`release` 뿐이라 [BuildProfile.QA] 는 도달하지 않는다.
 * QA 빌드 타입이나 플레이버가 생기면 `buildConfigField` 로 프로파일을 명시해 여기서 읽는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ObservabilityAppModule {
    @Provides
    @Singleton
    fun buildProfile(): BuildProfile = if (BuildConfig.DEBUG) BuildProfile.DEV else BuildProfile.PRODUCTION

    /**
     * Amplitude 수집 키. `local.properties` 의 `AMPLITUDE_API_KEY` 가 `buildConfigField` 로 들어온다.
     *
     * 비어 있으면 배선이 출구를 달지 않는다 — 키 없이 SDK 를 띄워 조용히 실패하는 것보다,
     * 아예 안 붙어서 로그에 안 보이는 편이 원인 추적이 빠르다.
     */
    @Provides
    @Singleton
    fun amplitudeApiKey(): AmplitudeApiKey = AmplitudeApiKey(BuildConfig.AMPLITUDE_API_KEY)
}
