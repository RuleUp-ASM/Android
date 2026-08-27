package com.ruleup.verification.data.db.geofence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

/**
 * 미전송분 드레인: tagPending(배치키) → byBatch → markSynced(명세 §2.4·§3.4). **미태깅 행으로 좁히면
 * 전송 실패분이 버퍼에 갇힌다**(#319) — 재전송이 안전한 근거는 서버 멱등(recordId · userId+signalType+observedAt).
 */
@Dao
interface GeofenceTransitionDao {
    @Insert
    suspend fun insert(entity: GeofenceTransitionEntity)

    @Query("UPDATE geofence_transition SET collectedAt = :key WHERE synced = 0")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM geofence_transition WHERE collectedAt = :key AND synced = 0")
    suspend fun byBatch(key: String): List<GeofenceTransitionEntity>

    @Query("UPDATE geofence_transition SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query("DELETE FROM geofence_transition WHERE synced = 1 AND occurredAt < :threshold")
    suspend fun purge(threshold: Long)
}

@Dao
interface LocationSampleDao {
    @Insert
    suspend fun insert(entity: LocationSampleEntity)

    @Query("UPDATE location_sample SET collectedAt = :key WHERE synced = 0")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM location_sample WHERE collectedAt = :key AND synced = 0")
    suspend fun byBatch(key: String): List<LocationSampleEntity>

    @Query("UPDATE location_sample SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query("DELETE FROM location_sample WHERE synced = 1 AND occurredAt < :threshold")
    suspend fun purge(threshold: Long)
}

@Dao
interface GeofenceTargetDao {
    @Query("SELECT * FROM geofence_target")
    suspend fun all(): List<GeofenceTargetEntity>

    @Upsert
    suspend fun upsertAll(items: List<GeofenceTargetEntity>)

    @Query("SELECT * FROM geofence_target WHERE requestId LIKE :prefix || '%'")
    suspend fun byRequestIdPrefix(prefix: String): List<GeofenceTargetEntity>

    @Query("DELETE FROM geofence_target WHERE requestId LIKE :prefix || '%'")
    suspend fun deleteByRequestIdPrefix(prefix: String)

    @Query("DELETE FROM geofence_target")
    suspend fun clear()
}
