package com.ruleup.verification.data.signal.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.ruleup.verification.data.signal.geofence.hasBackgroundLocation
import com.ruleup.verification.data.signal.health.HealthPermissions
import com.ruleup.verification.data.signal.usage.hasUsageAccess
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 신호별 권한 현황 스냅샷 채집(전송 스펙 §0.1·§0.4). 매 flush 마다 현재 grant 상태를 읽어
 * envelope `permissions` 로 보낸다. 회수/복구가 여기서 반영된다.
 *
 * Health Connect 권한은 런타임 checkSelfPermission 이 아니라 권한 컨트롤러의 grant 집합으로 확인한다.
 */
class PermissionSnapshotProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PermissionStatusProvider {
        override suspend fun capture(): PermissionSnapshot {
            val hc = healthConnectGranted()
            return PermissionSnapshot(
                location = context.statusOf(Manifest.permission.ACCESS_FINE_LOCATION),
                backgroundLocation = if (context.hasBackgroundLocation()) PermissionState.GRANTED else PermissionState.DENIED,
                activityRecognition = activityRecognitionStatus(),
                usageStats = if (context.hasUsageAccess()) PermissionState.GRANTED else PermissionState.DENIED,
                postNotifications = postNotificationsStatus(),
                healthDistance = hc.state(HealthPermission.getReadPermission(DistanceRecord::class)),
                healthSteps = hc.state(HealthPermission.getReadPermission(StepsRecord::class)),
                healthSleep = hc.state(HealthPermission.getReadPermission(SleepSessionRecord::class)),
                healthBackground = hc.state(PERMISSION_HEALTH_BACKGROUND),
            )
        }

        private suspend fun healthConnectGranted(): Set<String> {
            val client = HealthPermissions.clientOrNull(context) ?: return emptySet()
            return try {
                client.permissionController.getGrantedPermissions()
            } catch (e: SecurityException) {
                emptySet()
            } catch (e: IllegalStateException) {
                emptySet()
            }
        }

        private fun Set<String>.state(permission: String): PermissionState =
            if (contains(permission)) PermissionState.GRANTED else PermissionState.DENIED

        private fun Context.statusOf(permission: String): PermissionState =
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                PermissionState.GRANTED
            } else {
                PermissionState.DENIED
            }

        // ACTIVITY_RECOGNITION 은 API 29+ 런타임 권한, 이하에서는 설치 시 부여로 본다.
        private fun activityRecognitionStatus(): PermissionState =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                PermissionState.GRANTED
            } else {
                context.statusOf(Manifest.permission.ACTIVITY_RECOGNITION)
            }

        // POST_NOTIFICATIONS 는 API 33+ 런타임 권한, 이하에서는 부여 불필요.
        private fun postNotificationsStatus(): PermissionState =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                PermissionState.GRANTED
            } else {
                context.statusOf(Manifest.permission.POST_NOTIFICATIONS)
            }

        private companion object {
            // androidx.health 백그라운드 읽기 권한 문자열(grant 집합과 비교).
            const val PERMISSION_HEALTH_BACKGROUND = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        }
    }
