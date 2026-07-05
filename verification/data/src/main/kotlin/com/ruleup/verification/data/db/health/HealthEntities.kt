package com.ruleup.verification.data.db.health

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * 움직임(HEALTH) 읽기 버퍼(명세 §8). Health Connect 는 하루치 누적이 sync 마다 갱신되므로
 * 매 capture 마다 미태깅 스냅샷을 갈아끼우고(deleteUntagged→insert) 최신값을 재전송한다.
 * [date] 는 로컬 귀속 날짜(YYYY-MM-DD), [occurredAt] 은 기록 종료 epoch millis(정렬·TTL).
 */
@Entity(tableName = "health_reading")
data class HealthReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // DISTANCE / STEPS / EXERCISE_DURATION
    val metric: String,
    val value: Double,
    // km / count / min
    val unit: String,
    // EXERCISE 계열만 채움(예: RUNNING), 그 외 null
    val exerciseType: String?,
    val dataOrigin: String,
    // AUTO / ACTIVE / MANUAL / UNKNOWN
    val recordingMethod: String,
    // PHONE / WATCH / UNKNOWN
    val deviceType: String,
    val date: String,
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 수면 세그먼트 버퍼(명세 §6.2). 익일 배치라 lag 가 길다. */
@Entity(tableName = "sleep_segment")
data class SleepSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startAt: Long,
    val endAt: Long,
    val status: String,
    // 정렬·TTL용(= endAt)
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 움직임 수집 대상(스코프 소스, 명세 §3.2·§8). [metric] PK. */
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

    @Query("UPDATE health_reading SET collectedAt = :key WHERE synced = 0 AND collectedAt IS NULL")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM health_reading WHERE collectedAt = :key AND synced = 0 ORDER BY occurredAt ASC")
    suspend fun byBatch(key: String): List<HealthReadingEntity>

    @Query("UPDATE health_reading SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query("DELETE FROM health_reading WHERE synced = 1 AND occurredAt < :threshold")
    suspend fun purge(threshold: Long)
}

@Dao
interface SleepSegmentDao {
    @Insert
    suspend fun insertAll(items: List<SleepSegmentEntity>)

    @Query("DELETE FROM sleep_segment WHERE synced = 0 AND collectedAt IS NULL")
    suspend fun deleteUntagged()

    @Query("UPDATE sleep_segment SET collectedAt = :key WHERE synced = 0 AND collectedAt IS NULL")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM sleep_segment WHERE collectedAt = :key AND synced = 0 ORDER BY startAt ASC")
    suspend fun byBatch(key: String): List<SleepSegmentEntity>

    @Query("UPDATE sleep_segment SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query("DELETE FROM sleep_segment WHERE synced = 1 AND occurredAt < :threshold")
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
