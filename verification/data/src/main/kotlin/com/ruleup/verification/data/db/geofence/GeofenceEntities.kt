package com.ruleup.verification.data.db.geofence

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 지오펜스 전이 버퍼. */
@Entity(tableName = "geofence_transition")
data class GeofenceTransitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestId: String,
    // ENTER / EXIT / DWELL
    val transition: String,
    val accuracy: Float? = null,
    val isMock: Boolean? = null,
    val occurredAt: Long,
    // 수신 시점 SystemClock.elapsedRealtime()
    val observedElapsedMillis: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 보조 측위 샘플 버퍼. */
@Entity(tableName = "location_sample")
data class LocationSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val isMock: Boolean,
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 활성 지오펜스 목표(desired set). */
@Entity(tableName = "geofence_target")
data class GeofenceTargetEntity(
    @PrimaryKey
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
)
