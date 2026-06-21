package com.ruleup.verification.data.di

import android.content.Context
import com.ruleup.verification.data.db.GeofenceTargetDao
import com.ruleup.verification.data.db.GeofenceTransitionDao
import com.ruleup.verification.data.db.LocationSampleDao
import com.ruleup.verification.data.db.ProgressCacheDao
import com.ruleup.verification.data.db.UsageCursorDao
import com.ruleup.verification.data.db.UsageEventDao
import com.ruleup.verification.data.db.UsageTargetDao
import com.ruleup.verification.data.db.VerificationDatabase
import com.ruleup.verification.data.db.verificationDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * 로컬 버퍼 DB/DAO 를 그래프에 제공한다(androidMain 전용). Context 는 AndroidAppGraph 가 주입한다.
 * 리시버와 동일 인스턴스를 쓰도록 [verificationDatabase] 홀더 접근자를 거친다.
 */
@ContributesTo(AppScope::class)
interface VerificationDatabaseModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideVerificationDatabase(context: Context): VerificationDatabase = verificationDatabase(context)

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
}
