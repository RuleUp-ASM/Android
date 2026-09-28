package com.ruleup.verification.presentation.location.map

/** 지도 좌표(플랫폼 SDK 비의존). */
data class MapLatLng(
    val lat: Double,
    val lng: Double,
)

/** 지도에 고정 표시할 앵커 1개(등록된 인증 장소). */
data class MapAnchor(
    val lat: Double,
    val lng: Double,
    val radiusM: Float,
)

/** "현재 위치" 단발 측위 포트(구현 = FusedLocation). */
interface LocationLocator {
    suspend fun locate(): MapLatLng?
}
