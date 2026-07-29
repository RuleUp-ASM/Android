package com.ruleup.verification.presentation.location.map

/** 지도 좌표(플랫폼 SDK 비의존). :core:map 추상화 경계 타입. */
data class MapLatLng(
    val lat: Double,
    val lng: Double,
)

/** 지도에 고정 표시할 앵커 1개(등록된 인증 장소). 번호 핀 + 반경 원으로 그려진다. */
data class MapAnchor(
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
)

/** 반경 클램프(명세 §5.3): 10m 는 GPS 오차보다 작아 영구 EXIT 오탐, 5km 는 상시 ENTER. */
const val MIN_RADIUS_M: Float = 50f
const val MAX_RADIUS_M: Float = 1000f

fun clampRadius(radiusM: Float): Float = radiusM.coerceIn(MIN_RADIUS_M, MAX_RADIUS_M)

/** "현재 위치" 단발 측위 포트(android = FusedLocation, iOS = null). */
interface LocationLocator {
    suspend fun locate(): MapLatLng?
}
