package com.ruleup.android_ruleup.di

import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.observability.domain.model.AmplitudeApiKey
import com.ruleup.observability.domain.model.BuildProfile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 관측 파이프라인이 요구하는 앱 단위 값. */
@Module
@InstallIn(SingletonComponent::class)
object ObservabilityAppModule {
    @Provides
    @Singleton
    fun buildProfile(): BuildProfile = if (BuildConfig.DEBUG) BuildProfile.DEV else BuildProfile.PRODUCTION

    /** Amplitude 수집 키. */
    @Provides
    @Singleton
    fun amplitudeApiKey(): AmplitudeApiKey = AmplitudeApiKey(BuildConfig.AMPLITUDE_API_KEY)
}
