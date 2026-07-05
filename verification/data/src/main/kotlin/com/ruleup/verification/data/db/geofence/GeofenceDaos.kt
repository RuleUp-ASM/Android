package com.ruleup.verification.data.db.geofence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

/**
 * 미전송분 드레인 패턴: tagPending(배치키 부여) → byBatch(조회) → markSynced(확정). (명세 §2.4·§3.4)
 */
@Dao
interface GeofenceTransitionDao {
    @Insert
    suspend fun insert(entity: GeofenceTransitionEntity)

    @Query("UPDATE geofence_transition SET collectedAt = :key WHERE synced = 0 AND collectedAt IS NULL")
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

    @Query("UPDATE location_sample SET collectedAt = :key WHERE synced = 0 AND collectedAt IS NULL")
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

    @Query("DELETE FROM geofence_target WHERE requestId = :requestId")
    suspend fun deleteByRequestId(requestId: String)

    @Query("DELETE FROM geofence_target")
    suspend fun clear()
}
