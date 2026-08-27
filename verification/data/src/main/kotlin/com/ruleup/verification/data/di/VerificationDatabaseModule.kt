package com.ruleup.verification.data.di

import android.content.Context
import com.ruleup.verification.data.db.common.ProgressCacheDao
import com.ruleup.verification.data.db.common.SignalGapDao
import com.ruleup.verification.data.db.common.VerificationDatabase
import com.ruleup.verification.data.db.common.verificationDatabase
import com.ruleup.verification.data.db.geofence.GeofenceTargetDao
import com.ruleup.verification.data.db.geofence.GeofenceTransitionDao
import com.ruleup.verification.data.db.geofence.LocationSampleDao
import com.ruleup.verification.data.db.health.HealthReadingDao
import com.ruleup.verification.data.db.health.HealthTargetDao
import com.ruleup.verification.data.db.health.SleepSessionDao
import com.ruleup.verification.data.db.usage.UsageCursorDao
import com.ruleup.verification.data.db.usage.UsageEventDao
import com.ruleup.verification.data.db.usage.UsageTargetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 리시버와 동일 인스턴스를 쓰도록 [verificationDatabase] 홀더 접근자를 거친다. */
@Module
@InstallIn(SingletonComponent::class)
object VerificationDatabaseModule {
    @Provides
    @Singleton
    fun provideVerificationDatabase(
        @ApplicationContext context: Context,
    ): VerificationDatabase = verificationDatabase(context)

    @Provides
    fun provideGeofenceTransitionDao(database: VerificationDatabase): GeofenceTransitionDao = database.geofenceTransitionDao()

    @Provides
    fun provideLocationSampleDao(database: VerificationDatabase): LocationSampleDao = database.locationSampleDao()

    @Provides
    fun provideGeofenceTargetDao(database: VerificationDatabase): GeofenceTargetDao = database.geofenceTargetDao()

    @Provides
    fun provideProgressCacheDao(database: VerificationDatabase): ProgressCacheDao = database.progressCacheDao()

    @Provides
    fun provideUsageEventDao(database: VerificationDatabase): UsageEventDao = database.usageEventDao()

    @Provides
    fun provideUsageTargetDao(database: VerificationDatabase): UsageTargetDao = database.usageTargetDao()

    @Provides
    fun provideUsageCursorDao(database: VerificationDatabase): UsageCursorDao = database.usageCursorDao()

    @Provides
    fun provideHealthReadingDao(database: VerificationDatabase): HealthReadingDao = database.healthReadingDao()

    @Provides
    fun provideSleepSessionDao(database: VerificationDatabase): SleepSessionDao = database.sleepSessionDao()

    @Provides
    fun provideHealthTargetDao(database: VerificationDatabase): HealthTargetDao = database.healthTargetDao()

    @Provides
    fun provideSignalGapDao(database: VerificationDatabase): SignalGapDao = database.signalGapDao()
}
