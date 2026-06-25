package com.ruleup.verification.data.di

import android.content.Context
import com.ruleup.verification.data.db.GeofenceTargetDao
import com.ruleup.verification.data.db.GeofenceTransitionDao
import com.ruleup.verification.data.db.HealthReadingDao
import com.ruleup.verification.data.db.HealthTargetDao
import com.ruleup.verification.data.db.LocationSampleDao
import com.ruleup.verification.data.db.ProgressCacheDao
import com.ruleup.verification.data.db.SleepSegmentDao
import com.ruleup.verification.data.db.UsageCursorDao
import com.ruleup.verification.data.db.UsageEventDao
import com.ruleup.verification.data.db.UsageTargetDao
import com.ruleup.verification.data.db.VerificationDatabase
import com.ruleup.verification.data.db.verificationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 로컬 버퍼 DB/DAO 를 Hilt 그래프에 제공한다.
 * 리시버와 동일 인스턴스를 쓰도록 [verificationDatabase] 홀더 접근자를 거친다.
 */
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
    fun provideSleepSegmentDao(database: VerificationDatabase): SleepSegmentDao = database.sleepSegmentDao()

    @Provides
    fun provideHealthTargetDao(database: VerificationDatabase): HealthTargetDao = database.healthTargetDao()
}
