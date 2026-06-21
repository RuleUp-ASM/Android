package com.ruleup.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 지도 + 중심 핀 + 반경 원 렌더러(명세 §5.3). 탭/드래그로 중심을 옮긴다.
 * 플랫폼별 actual: android = Google Maps Compose, iOS = 플레이스홀더(자동인증 GPS 단독, 후속).
 */
@Composable
expect fun GeofenceMap(
    center: MapLatLng,
    radiusM: Float,
    onCenterChange: (MapLatLng) -> Unit,
    modifier: Modifier,
)

/** "현재 위치" 측위 포트 인스턴스. android 는 LocalContext 로 FusedLocation 을 잡는다. */
@Composable
expect fun rememberLocationLocator(): LocationLocator

/** 위치 권한 허용 여부(명세 §5.4.2: 미허용이면 "현재 위치" 버튼만 비활성, 수동 핀은 허용). */
@Composable
expect fun rememberLocationPermissionGranted(): Boolean
