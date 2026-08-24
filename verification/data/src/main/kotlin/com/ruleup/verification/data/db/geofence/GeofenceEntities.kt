package com.ruleup.verification.data.db.geofence

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 지오펜스 전이 버퍼 (전송 스펙 §1). 리시버가 앱이 죽어도 보존되도록 즉시 적재하고 sync 가 드레인한다.
 * [collectedAt] 은 드레인 시 부여되는 멱등 배치 태그(null=미드레인). [occurredAt] 은 epoch millis.
 *
 * **좌표는 담지 않는다** — 전송 계약에 lat/lng 가 없어 보낼 곳이 없고, 쓰지 않는 위치를 로컬에
 * 쌓아둘 이유도 없다. 좌표가 필요한 3순위 좌표 가중 체류는 `location_sample` 이 따로 맡는다.
 *
 * 정확도·mock 여부는 **없을 수 있다** — OS 가 전이만 주고 위치를 안 실어 보내는 경우가 있다.
 * 결측을 기본값으로 채우면 정확도 0m·"mock 아님"이라는 없던 사실이 만들어진다.
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
