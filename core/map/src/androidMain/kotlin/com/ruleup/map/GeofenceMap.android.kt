package com.ruleup.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@Composable
actual fun GeofenceMap(
    center: MapLatLng,
    radiusM: Float,
    onCenterChange: (MapLatLng) -> Unit,
    modifier: Modifier,
) {
    val latLng = LatLng(center.lat, center.lng)
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(latLng, DEFAULT_ZOOM)
        }
    val markerState = remember { MarkerState(position = latLng) }

    // 외부에서 center 가 바뀌면(현재 위치 점프 등) 카메라·핀을 따라가게 한다.
    LaunchedEffect(center) {
        val target = LatLng(center.lat, center.lng)
        markerState.position = target
        cameraPositionState.position = CameraPosition.fromLatLngZoom(target, cameraPositionState.position.zoom)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // 탭으로 중심 이동(명세 §5.3).
        onMapClick = { tapped -> onCenterChange(MapLatLng(tapped.latitude, tapped.longitude)) },
    ) {
        Marker(state = markerState)
        Circle(
            center = latLng,
            radius = radiusM.toDouble(),
            strokeColor = Color(0xFF3D5AFE),
            fillColor = Color(0x223D5AFE),
            strokeWidth = 3f,
        )
    }
}

@Composable
actual fun rememberLocationLocator(): LocationLocator {
    val context = LocalContext.current
    return remember(context) { FusedLocationLocator(context) }
}

@Composable
actual fun rememberLocationPermissionGranted(): Boolean {
    val context = LocalContext.current
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private class FusedLocationLocator(
    context: Context,
) : LocationLocator {
    private val appContext = context.applicationContext
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    override suspend fun locate(): MapLatLng? {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val location =
                fused
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                    .await()
            location?.let { MapLatLng(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        }
    }
}

private const val DEFAULT_ZOOM = 15f
