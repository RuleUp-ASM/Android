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
 * Health Connect 읽기 권한 요청 런처.
 *
 * 인증 화면(재연결)과 챌린지 화면(참여 전 권한 시트)이 **둘 다** 쓰기 때문에 core 에 둔다.
 * feature 를 모르고 HC 라이브러리만 쓴다.
 *
 * 위치가 `RequestMultiplePermissions` 를, 앱 사용시간이 설정 Intent 를 화면에서 직접 쓰는 것과 **같은
 * 방식**이다 — HC 도 자체 권한 컨트롤러를 화면이 직접 연다. 요청 결과는 신뢰하지 않고 호출부가 권한
 * 현황을 다시 조회한다(사용자가 일부만 허용할 수 있다).
 */
@Composable
fun rememberHealthPermissionLauncher(onResult: () -> Unit): ManagedActivityResultLauncher<Set<String>, Set<String>> =
    rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { onResult() }

/** 걸음·거리·수면 읽기 권한. 요청 화면에 한 번에 올린다. */
fun healthReadPermissions(): Set<String> =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

/**
 * 이 기기에서 Health Connect 권한을 요청할 수 있는가.
 *
 * 미설치·미지원이면 요청 화면 자체가 뜨지 않는다 — 그때는 버튼을 눌러도 아무 일이 없는 대신
 * 안내로 떨어뜨려야 한다.
 */
@Composable
fun healthConnectAvailable(): Boolean = healthConnectAvailable(LocalContext.current)

fun healthConnectAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
