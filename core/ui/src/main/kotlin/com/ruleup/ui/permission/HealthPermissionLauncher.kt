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

/**
 * Health Connect 읽기 권한 요청 런처. 인증·챌린지 화면이 둘 다 써서 core 에 있다.
 * 반환된 결과는 믿지 말고 호출부가 권한 현황을 다시 조회한다 — 사용자가 일부만 허용할 수 있다.
 */
@Composable
fun rememberHealthPermissionLauncher(onResult: () -> Unit): ManagedActivityResultLauncher<Set<String>, Set<String>> =
    rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { onResult() }

fun healthReadPermissions(): Set<String> =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

/** false 면 요청 화면 자체가 뜨지 않는다 — 버튼을 그대로 두지 말고 안내로 떨어뜨려야 한다. */
@Composable
fun healthConnectAvailable(): Boolean = healthConnectAvailable(LocalContext.current)

fun healthConnectAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
