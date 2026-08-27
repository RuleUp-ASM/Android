package com.ruleup.verification.data.db.geofence

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 지오펜스 전이 버퍼(전송 스펙 §1). 앱이 죽어도 남도록 리시버가 즉시 적재하고 sync 가 드레인한다
 * ([collectedAt]=배치 태그, null=미드레인). 좌표는 계약에 없어 담지 않는다(좌표는 `location_sample` 소관).
 * 정확도·mock 은 OS 가 위치를 안 준 전이에서 null — 0m·"mock 아님"으로 접으면 없던 사실이 판정에 들어간다.
 */
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
    // 수신 시점 SystemClock.elapsedRealtime() — 시각 조작 교차검증(전송 스펙 §6.4)
    val observedElapsedMillis: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** 보조 측위 샘플 버퍼 (명세 §2.3). sanity·mock 교차검증용. */
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

/**
 * 활성 지오펜스 목표(desired set). OS 등록은 재부팅·위치설정 토글 시 휘발되므로(명세 §2.3)
 * 목표를 로컬에 보존해 BOOT_COMPLETED·콜드스타트에서 reconcile 재등록의 소스로 쓴다.
 */
@Entity(tableName = "geofence_target")
data class GeofenceTargetEntity(
    @PrimaryKey
    val requestId: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
    val dwellMinutes: Int,
)
