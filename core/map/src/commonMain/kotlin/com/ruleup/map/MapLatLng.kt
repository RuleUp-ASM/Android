package com.ruleup.map

/** 지도 좌표(플랫폼 SDK 비의존). :core:map 추상화 경계 타입. */
data class MapLatLng(
    val lat: Double,
    val lng: Double,
)

/** 반경 클램프(명세 §5.3): 10m 는 GPS 오차보다 작아 영구 EXIT 오탐, 5km 는 상시 ENTER. */
const val MIN_RADIUS_M: Float = 50f
const val MAX_RADIUS_M: Float = 1000f

fun clampRadius(radiusM: Float): Float = radiusM.coerceIn(MIN_RADIUS_M, MAX_RADIUS_M)

/** "현재 위치" 단발 측위 포트(android = FusedLocation, iOS = null). */
interface LocationLocator {
    suspend fun locate(): MapLatLng?
}
