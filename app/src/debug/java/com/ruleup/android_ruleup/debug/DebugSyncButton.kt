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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ruleup.ui.helper.rememberSingleClick
import com.ruleup.verification.data.signal.HealthPermissions
import com.ruleup.verification.data.signal.hasUsageAccess
import com.ruleup.verification.data.signal.usageAccessSettingsIntent
import com.ruleup.verification.domain.port.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

// 수집·동기화 로그 태그(Worker/Repository 와 동일). 디버그 오버레이/Logcat 에서 'VerifySync' 로 필터.
private const val LOG_TAG = "VerifySync"

/**
 * 디버그 빌드 전용 "지금 수집·동기화" 트리거. 누르면 수집을 막을 수 있는 권한을 모두 요청/안내한 뒤
 * expedited catch-up 으로 [com.ruleup.verification.data.sync.VerificationSyncWorker] 를 즉시 한 번 돌린다.
 *
 * OS 가 코드로의 조용한 grant 를 막으므로 순서대로 사용자에게 받아낸다:
 * 런타임(위치·활동인식·알림) → 백그라운드 위치(별도 단계) → Health Connect → 사용량 접근(설정 화면).
 * 사용량 접근은 설정에서 토글해야 하므로, 미허용이면 설정만 열고 안내한다(허용 후 다시 누르면 전체 수집).
 *
 * 결과는 Logcat + 화면 우측 상단 [DebugLogOverlay] 에 'VerifySync' 태그로 뜬다.
 */
@Composable
fun DebugSyncButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // HC 권한 요청 컨트랙트(verification:data 가 HC 클래스를 숨김 — 여기선 Set<String> 만 다룬다).
    val healthContract = remember { HealthPermissions.requestPermissionsContract() }

    val healthLauncher =
        rememberLauncherForActivityResult(healthContract) { granted ->
            Timber.tag(LOG_TAG).i("Health Connect 권한 — 허용 %d/%d", granted.size, HealthPermissions.readPermissions().size)
            handleUsageThenCollect(context)
        }
    val backgroundLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Timber.tag(LOG_TAG).i("백그라운드 위치 — %s", if (granted) "허용" else "거부")
            requestHealthThenCollect(context) { healthLauncher.launch(it) }
        }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            Timber.tag(LOG_TAG).i(
                "런타임 권한 — %s",
                result.entries.joinToString { "${it.key.substringAfterLast('.')}=${it.value}" },
            )
            // 포그라운드 위치 허용 후에만 백그라운드 위치를 별도 단계로 요청(API29+ 동시 요청 불가).
            if (needsBackgroundLocation(context)) {
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                requestHealthThenCollect(context) { healthLauncher.launch(it) }
            }
        }

    FilledTonalButton(
        onClick =
            rememberSingleClick {
                Timber.tag(LOG_TAG).i("▶ 권한 확인·요청 후 수집·동기화")
                runtimeLauncher.launch(runtimePermissions())
            },
        modifier = modifier,
    ) {
        Text("수집·동기화")
    }
}

// HC 사용 가능하면 권한 요청(콜백에서 이어짐), 미지원 기기면 건너뛰고 사용량 확인·수집으로.
private fun requestHealthThenCollect(
    context: Context,
    launchHealth: (Set<String>) -> Unit,
) {
    if (HealthPermissions.isAvailable(context)) {
        launchHealth(HealthPermissions.readPermissions())
    } else {
        Timber.tag(LOG_TAG).i("Health Connect 미지원 기기 — 건너뜀")
        handleUsageThenCollect(context)
    }
}

// 사용량 접근(특수)은 코드로 grant 불가 → 미허용이면 설정만 열고 안내(허용 후 다시 탭), 허용 상태면 수집 실행.
private fun handleUsageThenCollect(context: Context) {
    if (!context.hasUsageAccess()) {
        Timber.tag(LOG_TAG).w("사용량 접근 미허용 → 설정 열기. 허용 후 '수집·동기화' 다시 누르면 전체 수집")
        context.startActivity(usageAccessSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    enqueueCollect(context)
}

private fun enqueueCollect(context: Context) {
    val scheduler =
        EntryPointAccessors
            .fromApplication(context.applicationContext, DebugToolsEntryPoint::class.java)
            .syncScheduler()
    Timber.tag(LOG_TAG).i("✔ 권한 확인 완료 — catch-up 수집·동기화 enqueue")
    scheduler.enqueueCatchUp()
}

// 요청할 런타임 권한(OS 버전별 추가). 백그라운드 위치는 포그라운드 허용 후 별도 단계라 여기 넣지 않는다.
private fun runtimePermissions(): Array<String> =
    buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

// 포그라운드 위치는 허용됐고 백그라운드 위치만 아직 미허용인가(API29+).
private fun needsBackgroundLocation(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val fineGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val backgroundGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fineGranted && !backgroundGranted
}

/** [SyncScheduler] 를 컴포저블(비-Hilt)에서 꺼내기 위한 Hilt 진입점(디버그 트리거 전용). */
@EntryPoint
@InstallIn(SingletonComponent::class)
fun interface DebugToolsEntryPoint {
    fun syncScheduler(): SyncScheduler
}
