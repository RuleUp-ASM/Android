package com.ruleup.verification.data.db.geofence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

/** 미전송분 드레인: tagPending(배치키) → byBatch → markSynced. */
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

    @Query(
        "SELECT MIN(occurredAt) FROM geofence_transition WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM geofence_transition ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun oldestEvicted(threshold: Long): Long?

    @Query(
        "SELECT MAX(occurredAt) FROM geofence_transition WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM geofence_transition ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun newestEvicted(threshold: Long): Long?

    @Query(
        "DELETE FROM geofence_transition WHERE occurredAt < :threshold OR id NOT IN (SELECT id FROM geofence_transition ORDER BY occurredAt DESC, id DESC LIMIT 10000)",
    )
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

    @Query(
        "SELECT MIN(occurredAt) FROM location_sample WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM location_sample ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun oldestEvicted(threshold: Long): Long?

    @Query(
        "SELECT MAX(occurredAt) FROM location_sample WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM location_sample ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun newestEvicted(threshold: Long): Long?

    @Query(
        "DELETE FROM location_sample WHERE occurredAt < :threshold OR id NOT IN (SELECT id FROM location_sample ORDER BY occurredAt DESC, id DESC LIMIT 10000)",
    )
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
