package com.ruleup.ui.permission

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
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
