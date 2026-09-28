package com.ruleup.android_ruleup.logging

import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.logging.domain.BizLogConfig
import com.ruleup.logging.domain.BizScreenSource
import com.ruleup.logging.domain.BizUserSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 비즈니스 로깅이 요구하는 앱 단위 값과 출처. */
@Module
@InstallIn(SingletonComponent::class)
object BizLogAppModule {
    /** Amplitude 수집 키. */
    @Provides
    @Singleton
    fun bizLogConfig(): BizLogConfig =
        BizLogConfig(
            amplitudeApiKey = BuildConfig.AMPLITUDE_API_KEY,
            debuggable = BuildConfig.DEBUG,
        )

    /** 화면 출처는 `ScreenTracker` 가 갱신하는 홀더다 */
    @Provides
    @Singleton
    fun bizScreenSource(holder: ScreenPathHolder): BizScreenSource = holder

    @Provides
    @Singleton
    fun bizUserSource(holder: CurrentUserHolder): BizUserSource = holder
}
