package com.ruleup.verification.presentation.location.map

/** 지도 좌표(플랫폼 SDK 비의존). 화면과 지도 SDK 사이의 경계 타입이라 Kakao LatLng 를 새어 내보내지 않는다. */
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

/** "현재 위치" 단발 측위 포트(구현 = FusedLocation). 권한이 없거나 못 잡으면 null. */
interface LocationLocator {
    suspend fun locate(): MapLatLng?
}
