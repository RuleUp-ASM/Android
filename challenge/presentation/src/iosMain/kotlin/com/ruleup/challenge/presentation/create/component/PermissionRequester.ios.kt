package com.ruleup.challenge.presentation.create.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
actual fun rememberPermissionRequester(): PermissionRequester = remember { IosPermissionRequester() }

/**
 * iOS 권한 요청. 카메라/위치는 네이티브 API 로 요청하고, 매핑 안 되는 토큰은 낙관적으로 허용한다.
 * TODO(server-contract): 서버 권한 토큰 어휘 확정 시 매핑(모션·헬스 등) 보완.
 */
private class IosPermissionRequester : PermissionRequester {
    // 콜백 완료 전까지 매니저/델리게이트를 강참조로 보존(조기 해제 방지).
    private var locationManager: CLLocationManager? = null
    private var locationDelegate: NSObject? = null

    override suspend fun request(tokens: List<String>): Set<String> {
        val granted = mutableSetOf<String>()
        tokens.forEach { token ->
            val ok =
                when (token.uppercase()) {
                    "CAMERA", "PHOTO" -> requestCamera()
                    "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> requestLocation()
                    else -> true
                }
            if (ok) granted += token
        }
        return granted
    }

    private suspend fun requestCamera(): Boolean =
        suspendCancellableCoroutine { cont ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { isGranted ->
                cont.resume(isGranted)
            }
        }

    private suspend fun requestLocation(): Boolean =
        suspendCancellableCoroutine { cont ->
            val manager = CLLocationManager()
            val delegate =
                object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                        val status = manager.authorizationStatus
                        if (status == kCLAuthorizationStatusNotDetermined) return
                        locationManager = null
                        locationDelegate = null
                        if (cont.isActive) {
                            cont.resume(
                                status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                                    status == kCLAuthorizationStatusAuthorizedAlways,
                            )
                        }
                    }
                }
            locationManager = manager
            locationDelegate = delegate
            manager.delegate = delegate
            manager.requestWhenInUseAuthorization()
        }
}
