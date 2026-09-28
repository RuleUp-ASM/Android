package com.ruleup.verification.data.db.health

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.RecordingMethod

/** 움직임(HEALTH) 읽기 버퍼. */
@Entity(tableName = "health_reading")
data class HealthReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Health Connect metadata.id
    val recordId: String,
    val metric: HealthMetric,
    val value: Double,
    val startTime: Long,
    val endTime: Long,
    val originPackage: String,
    val recordingMethod: RecordingMethod,
    val date: String,
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 수면 세션 버퍼. */
@Entity(tableName = "sleep_session")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: String,
    val startAt: Long,
    val endAt: Long,
    val durationMillis: Long,
    val sleepMillis: Long?,
    val observedElapsedMillis: Long,
    val originPackage: String,
    val recordingMethod: RecordingMethod,
    // 정렬·TTL용(= endAt)
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 움직임 수집 대상. */
@Entity(tableName = "health_target")
data class HealthTargetEntity(
    @PrimaryKey
    val metric: String,
    val exerciseType: String?,
)

/** 수면 인증 유무 플래그(단일 행 id=0, SLEEP 수집 on/off). */
@Entity(tableName = "health_settings")
data class HealthSettingsEntity(
    @PrimaryKey
    val id: Int = 0,
    val sleepRequested: Boolean,
)

@Dao
interface HealthReadingDao {
    @Insert
    suspend fun insertAll(items: List<HealthReadingEntity>)

    /** 아직 배치에 안 묶인 미전송 스냅샷 제거(최신 스냅샷으로 교체하기 전에 호출). */
    @Query("DELETE FROM health_reading WHERE synced = 0 AND collectedAt IS NULL")
    suspend fun deleteUntagged()

    @Query("UPDATE health_reading SET collectedAt = :key WHERE synced = 0")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM health_reading WHERE collectedAt = :key AND synced = 0 ORDER BY occurredAt ASC")
    suspend fun byBatch(key: String): List<HealthReadingEntity>

    @Query("UPDATE health_reading SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query(
        "SELECT MIN(occurredAt) FROM health_reading WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM health_reading ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun oldestEvicted(threshold: Long): Long?

    @Query(
        "SELECT MAX(occurredAt) FROM health_reading WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM health_reading ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun newestEvicted(threshold: Long): Long?

    @Query(
        "DELETE FROM health_reading WHERE occurredAt < :threshold OR id NOT IN (SELECT id FROM health_reading ORDER BY occurredAt DESC, id DESC LIMIT 10000)",
    )
    suspend fun purge(threshold: Long)
}

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insertAll(items: List<SleepSessionEntity>)

    @Query("DELETE FROM sleep_session WHERE synced = 0 AND collectedAt IS NULL")
    suspend fun deleteUntagged()

    @Query("UPDATE sleep_session SET collectedAt = :key WHERE synced = 0")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM sleep_session WHERE collectedAt = :key AND synced = 0 ORDER BY startAt ASC")
    suspend fun byBatch(key: String): List<SleepSessionEntity>

    @Query("UPDATE sleep_session SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query(
        "SELECT MIN(occurredAt) FROM sleep_session WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM sleep_session ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun oldestEvicted(threshold: Long): Long?

    @Query(
        "SELECT MAX(occurredAt) FROM sleep_session WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM sleep_session ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun newestEvicted(threshold: Long): Long?

    @Query(
        "DELETE FROM sleep_session WHERE occurredAt < :threshold OR id NOT IN (SELECT id FROM sleep_session ORDER BY occurredAt DESC, id DESC LIMIT 10000)",
    )
    suspend fun purge(threshold: Long)
}

@Dao
interface HealthTargetDao {
    @Query("SELECT * FROM health_target")
    suspend fun all(): List<HealthTargetEntity>

    @Upsert
    suspend fun upsertAll(items: List<HealthTargetEntity>)

    @Query("DELETE FROM health_target")
    suspend fun clear()

    @Query("SELECT sleepRequested FROM health_settings WHERE id = 0")
    suspend fun sleepRequested(): Boolean?

    @Upsert
    suspend fun setSettings(settings: HealthSettingsEntity)
}
