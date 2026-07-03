package com.ruleup.analytics.data

import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.analytics.domain.CrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsLoggerModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsLogger(impl: AndroidAnalyticsLogger): AnalyticsLogger

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter
}
