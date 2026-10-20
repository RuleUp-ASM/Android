package com.ruleup.android_ruleup.tti

import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.observability.domain.api.Observability
import com.ruleup.tti.domain.TtiShooter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** TTI 가 어디로 나갈지 정하는 곳. */
@Module
@InstallIn(SingletonComponent::class)
object TtiAppModule {
    @Provides
    @Singleton
    fun provideTtiShooter(
        observabilityShooter: ObservabilityTtiShooter,
        deviceInfoProvider: DeviceInfoProvider,
        observability: Observability,
    ): TtiShooter {
        // 앱 API 용 클라이언트를 쓰면 인증 토큰이 구글로 따라간다.
        val sheetShooter =
            SheetTtiShooter(
                url = BuildConfig.TTI_SHEET_URL,
                buildType = BuildConfig.BUILD_TYPE,
                client = OkHttpClient(),
                deviceInfoProvider = deviceInfoProvider,
                observability = observability,
            )
        return TtiShooter { records ->
            observabilityShooter.shoot(records)
            sheetShooter.shoot(records)
        }
    }
}
