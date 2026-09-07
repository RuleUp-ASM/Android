package com.ruleup.verification.data.db.common

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ruleup.verification.data.db.geofence.GeofenceTargetDao
import com.ruleup.verification.data.db.geofence.GeofenceTargetEntity
import com.ruleup.verification.data.db.geofence.GeofenceTransitionDao
import com.ruleup.verification.data.db.geofence.GeofenceTransitionEntity
import com.ruleup.verification.data.db.geofence.LocationSampleDao
import com.ruleup.verification.data.db.geofence.LocationSampleEntity
import com.ruleup.verification.data.db.health.HealthReadingDao
import com.ruleup.verification.data.db.health.HealthReadingEntity
import com.ruleup.verification.data.db.health.HealthSettingsEntity
import com.ruleup.verification.data.db.health.HealthTargetDao
import com.ruleup.verification.data.db.health.HealthTargetEntity
import com.ruleup.verification.data.db.health.SleepSessionDao
import com.ruleup.verification.data.db.health.SleepSessionEntity
import com.ruleup.verification.data.db.usage.UsageCursorDao
import com.ruleup.verification.data.db.usage.UsageCursorEntity
import com.ruleup.verification.data.db.usage.UsageEventDao
import com.ruleup.verification.data.db.usage.UsageEventEntity
import com.ruleup.verification.data.db.usage.UsageTargetDao
import com.ruleup.verification.data.db.usage.UsageTargetEntity

/**
 * 자동인증 로컬 버퍼 DB (단일 진실원, 명세 §2.4).
 * 마이그레이션 대신 파괴적 재생성으로 둔다.
 */
@Database(
    entities = [
        GeofenceTransitionEntity::class,
        LocationSampleEntity::class,
        GeofenceTargetEntity::class,
        ProgressCacheEntity::class,
        UsageEventEntity::class,
        UsageTargetEntity::class,
        UsageCursorEntity::class,
        HealthReadingEntity::class,
        SleepSessionEntity::class,
        HealthTargetEntity::class,
        HealthSettingsEntity::class,
        SignalGapEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(VerificationTypeConverters::class)
abstract class VerificationDatabase : RoomDatabase() {
    abstract fun geofenceTransitionDao(): GeofenceTransitionDao

    abstract fun locationSampleDao(): LocationSampleDao

    abstract fun geofenceTargetDao(): GeofenceTargetDao

    abstract fun progressCacheDao(): ProgressCacheDao

    abstract fun usageEventDao(): UsageEventDao

    abstract fun usageTargetDao(): UsageTargetDao

    abstract fun usageCursorDao(): UsageCursorDao

    abstract fun healthReadingDao(): HealthReadingDao

    abstract fun sleepSessionDao(): SleepSessionDao

    abstract fun healthTargetDao(): HealthTargetDao

    abstract fun signalGapDao(): SignalGapDao
}

/**
 * 프로세스 전역 단일 인스턴스 홀더.
 *
 * BroadcastReceiver(지오펜스·부팅)는 OS 가 인스턴스화하므로 Hilt 그래프에 접근할 수 없다.
 * Hilt DI 모듈과 리시버가 **같은** DB 파일/인스턴스를 공유하도록 본 홀더를 단일 출처로 둔다.
 */
internal object VerificationDatabaseHolder {
    @Volatile
    private var instance: VerificationDatabase? = null

    fun get(context: Context): VerificationDatabase =
        instance ?: synchronized(this) {
            instance ?: Room
                .databaseBuilder(
                    context.applicationContext,
                    VerificationDatabase::class.java,
                    "ruleup_verification.db",
                ).fallbackToDestructiveMigration(true)
                .build()
                .also { instance = it }
        }
}

internal fun verificationDatabase(context: Context): VerificationDatabase = VerificationDatabaseHolder.get(context)
