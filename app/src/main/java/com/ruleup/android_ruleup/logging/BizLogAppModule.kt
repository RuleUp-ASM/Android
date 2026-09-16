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

/**
 * 비즈니스 로깅이 요구하는 앱 단위 값과 출처.
 *
 * `:logging:data` 가 이 셋을 제공할 수 없다 — 수집 키와 빌드 종류는 앱의 `BuildConfig` 에만 있고,
 * 지금 화면은 네비게이션을, 사용자 식별자는 세션을 쥔 쪽만 안다.
 */
@Module
@InstallIn(SingletonComponent::class)
object BizLogAppModule {
    /**
     * Amplitude 수집 키는 `local.properties` 의 `AMPLITUDE_API_KEY` 가 `buildConfigField` 로 들어온다.
     * 비어 있으면 배선이 그 출구를 달지 않는다.
     */
    @Provides
    @Singleton
    fun bizLogConfig(): BizLogConfig =
        BizLogConfig(
            amplitudeApiKey = BuildConfig.AMPLITUDE_API_KEY,
            debuggable = BuildConfig.DEBUG,
        )

    /** 화면 출처는 `ScreenTracker` 가 갱신하는 홀더다 — 추적기를 직접 꽂으면 기록기와 순환이 된다. */
    @Provides
    @Singleton
    fun bizScreenSource(holder: ScreenPathHolder): BizScreenSource = holder

    @Provides
    @Singleton
    fun bizUserSource(holder: CurrentUserHolder): BizUserSource = holder
}
