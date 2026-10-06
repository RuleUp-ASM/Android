package com.ruleup.ui.permission

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

/** Health Connect 읽기 권한 요청 런처. */
@Composable
fun rememberHealthPermissionLauncher(onResult: () -> Unit): ManagedActivityResultLauncher<Set<String>, Set<String>> =
    rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { onResult() }

fun healthReadPermissions(): Set<String> =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

/** false 면 요청 화면 자체가 뜨지 않는다 */
@Composable
fun healthConnectAvailable(): Boolean = healthConnectAvailable(LocalContext.current)

fun healthConnectAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

/** 앱이 꺼져 있을 때 읽기 권한. */
const val HEALTH_BACKGROUND_PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

/** 앱이 꺼져 있을 때도 읽을 수 있는 기기인가. 아니면 앱을 열었을 때만 모은다. */
fun healthBackgroundReadAvailable(context: Context): Boolean =
    healthConnectAvailable(context) &&
        HealthConnectClient
            .getOrCreate(context)
            .features
            .getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
        HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

/**
 * 실제로 요청할 권한 묶음. 백그라운드 읽기는 지원 기기에서만 같이 묻는다 —
 * 빠지면 주기 sync 가 앱이 꺼진 동안 아무것도 못 읽는다(#567).
 */
fun healthRequestPermissions(
    context: Context,
    requested: Set<String> = healthReadPermissions(),
): Set<String> =
    if (requested.isNotEmpty() && healthBackgroundReadAvailable(context)) {
        requested + HEALTH_BACKGROUND_PERMISSION
    } else {
        requested - HEALTH_BACKGROUND_PERMISSION
    }
