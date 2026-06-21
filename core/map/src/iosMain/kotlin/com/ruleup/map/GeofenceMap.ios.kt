package com.ruleup.map

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * iOS 플레이스홀더. 자동인증이 iOS 에선 GPS 단독으로 축소되고 지도 POC 는 Android 우선이라,
 * 컴파일 대칭만 맞춘다(명세 §0·§8). 실제 지도는 후속(CoreLocation/MapKit).
 */
@Composable
actual fun GeofenceMap(
    center: MapLatLng,
    radiusM: Float,
    onCenterChange: (MapLatLng) -> Unit,
    modifier: Modifier,
) {
    Box(modifier) {
        Text("지도는 Android 에서 지원됩니다")
    }
}

@Composable
actual fun rememberLocationLocator(): LocationLocator =
    remember {
        object : LocationLocator {
            override suspend fun locate(): MapLatLng? = null
        }
    }

@Composable
actual fun rememberLocationPermissionGranted(): Boolean = false
