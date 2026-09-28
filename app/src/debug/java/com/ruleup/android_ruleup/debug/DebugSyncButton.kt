package com.ruleup.android_ruleup.debug

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.ruleup.designsystem.rememberSingleClick
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.api.w
import com.ruleup.ui.helper.LocalObservability
import com.ruleup.verification.data.signal.health.HealthPermissions
import com.ruleup.verification.data.signal.usage.hasUsageAccess
import com.ruleup.verification.data.signal.usage.usageAccessSettingsIntent
import com.ruleup.verification.data.sync.VerificationSyncSchedulerImpl

// 수집·동기화 로그 태그(Worker/Repository 와 동일).
private const val LOG_TAG = "VerifySync"

/** 디버그 빌드 전용 "지금 수집·동기화" 트리거. */
@Composable
fun DebugSyncButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val observability = LocalObservability.current

    // 워커가 실제로 도는지·몇 번째 시도인지 눈으로 확인하려고 상태 전이를 로그로 흘린다.
    LaunchedEffect(Unit) {
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWorkFlow(VerificationSyncSchedulerImpl.CATCH_UP_WORK_NAME)
            .collect { infos ->
                val info = infos.lastOrNull() ?: return@collect
                observability.i(LOG_TAG) { "catch-up 상태 — ${info.state} (시도 ${info.runAttemptCount}회)" }
            }
    }

    // HC 권한 요청 컨트랙트(verification:data 가 HC 클래스를 숨김 — 여기선 Set<String> 만 다룬다).
    val healthContract = remember { HealthPermissions.requestPermissionsContract() }

    val healthLauncher =
        rememberLauncherForActivityResult(healthContract) { granted ->
            observability.i(LOG_TAG) { "Health Connect 권한 — 허용 ${granted.size}/${HealthPermissions.readPermissions().size}" }
            handleUsageThenCollect(context, observability)
        }
    val backgroundLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            observability.i(LOG_TAG) { "백그라운드 위치 — ${if (granted) "허용" else "거부"}" }
            requestHealthThenCollect(context, observability) { healthLauncher.launch(it) }
        }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            observability.i(LOG_TAG) {
                "런타임 권한 — " + result.entries.joinToString { "${it.key.substringAfterLast('.')}=${it.value}" }
            }
            // 포그라운드 위치 허용 후에만 백그라운드 위치를 별도 단계로 요청(API29+ 동시 요청 불가).
            if (needsBackgroundLocation(context)) {
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                requestHealthThenCollect(context, observability) { healthLauncher.launch(it) }
            }
        }

    FilledTonalButton(
        onClick =
            rememberSingleClick {
                observability.i(LOG_TAG) { "▶ 권한 확인·요청 후 수집·동기화" }
                runtimeLauncher.launch(runtimePermissions())
            },
        modifier = modifier,
    ) {
        Text("수집·동기화")
    }
}

private fun requestHealthThenCollect(
    context: Context,
    observability: Observability,
    launchHealth: (Set<String>) -> Unit,
) {
    if (HealthPermissions.isAvailable(context)) {
        launchHealth(HealthPermissions.readPermissions())
    } else {
        observability.i(LOG_TAG) { "Health Connect 미지원 기기 — 건너뜀" }
        handleUsageThenCollect(context, observability)
    }
}

// 사용량 접근(특수)은 코드로 grant 불가 → 미허용이면 설정만 열고 안내(허용 후 다시 탭), 허용 상태면 수집 실행.
private fun handleUsageThenCollect(
    context: Context,
    observability: Observability,
) {
    if (!context.hasUsageAccess()) {
        observability.w(LOG_TAG) { "사용량 접근 미허용 → 설정 열기. 허용 후 '수집·동기화' 다시 누르면 전체 수집" }
        context.startActivity(usageAccessSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    enqueueCollect(context, observability)
}

private fun enqueueCollect(
    context: Context,
    observability: Observability,
) {
    // 디버그 수동 트리거는 REPLACE
    observability.i(LOG_TAG) { "✔ 권한 확인 완료 — catch-up 수집·동기화 enqueue (REPLACE)" }
    VerificationSyncSchedulerImpl.enqueueCatchUp(context, ExistingWorkPolicy.REPLACE)
}

// 요청할 런타임 권한(OS 버전별 추가).
private fun runtimePermissions(): Array<String> =
    buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

private fun needsBackgroundLocation(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val fineGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val backgroundGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fineGranted && !backgroundGranted
}
