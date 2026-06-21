package com.ruleup.verification.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 자동인증 로컬 버퍼 DB (단일 진실원, 명세 §2.4).
 * POC 단계라 마이그레이션 대신 파괴적 재생성으로 둔다(스키마 변동 잦음).
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
    ],
    version = 3,
    exportSchema = false,
)
abstract class VerificationDatabase : RoomDatabase() {
    abstract fun geofenceTransitionDao(): GeofenceTransitionDao

    abstract fun locationSampleDao(): LocationSampleDao

    abstract fun geofenceTargetDao(): GeofenceTargetDao

    abstract fun progressCacheDao(): ProgressCacheDao

    abstract fun usageEventDao(): UsageEventDao

    abstract fun usageTargetDao(): UsageTargetDao

    abstract fun usageCursorDao(): UsageCursorDao
}

/**
 * 프로세스 전역 단일 인스턴스 홀더.
 *
 * BroadcastReceiver(지오펜스·부팅)는 OS 가 인스턴스화하므로 Metro 그래프에 접근할 수 없다.
 * Metro DI 모듈과 리시버가 **같은** DB 파일/인스턴스를 공유하도록 본 홀더를 단일 출처로 둔다.
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

/** DI 모듈·리시버 공용 DB 접근자(단일 인스턴스). */
internal fun verificationDatabase(context: Context): VerificationDatabase = VerificationDatabaseHolder.get(context)
