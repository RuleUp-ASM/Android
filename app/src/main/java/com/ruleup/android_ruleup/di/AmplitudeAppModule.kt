package com.ruleup.android_ruleup.di

import android.app.Application
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.common.Logger
import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.remoteconfig.data.RemoteConfigDeployment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AmplitudeAppModule {
    @Provides
    @Singleton
    fun analytics(application: Application): Amplitude =
        Amplitude(Configuration(apiKey = BuildConfig.AMPLITUDE_API_KEY, context = application)).also {
            if (BuildConfig.DEBUG) it.logger.logMode = Logger.LogMode.DEBUG
        }

    @Provides
    fun remoteConfigDeployment(): RemoteConfigDeployment =
        RemoteConfigDeployment(
            analyticsKey = BuildConfig.AMPLITUDE_API_KEY,
            experimentKey = BuildConfig.AMPLITUDE_EXPERIMENT_KEY,
        )
}
