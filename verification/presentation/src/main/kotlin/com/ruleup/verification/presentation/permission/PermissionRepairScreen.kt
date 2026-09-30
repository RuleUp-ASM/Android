package com.ruleup.verification.presentation.permission

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.permission.healthConnectAvailable
import com.ruleup.ui.permission.healthReadPermissions
import com.ruleup.ui.permission.rememberHealthPermissionLauncher
import com.ruleup.verification.domain.entity.PermissionRequestKind
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.presentation.permission.viewmodel.PermissionRepairIntent
import com.ruleup.verification.presentation.permission.viewmodel.PermissionRepairViewModel

/** 권한 재연결. */
@Composable
fun PermissionRepairScreen(
    modifier: Modifier = Modifier,
    requiredPermissions: List<String>? = null,
    viewModel: PermissionRepairViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.permissions == null)
    val context = LocalContext.current
    // 설정 복귀 시 권한 재조회.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(PermissionRepairIntent.Refresh)
    }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onIntent(PermissionRepairIntent.Refresh)
        }
    // Health Connect 권한 요청.
    val healthLauncher = rememberHealthPermissionLauncher { viewModel.onIntent(PermissionRepairIntent.Refresh) }
    val healthAvailable = healthConnectAvailable()

    val rows =
        state.permissions?.let(::repairRows).orEmpty().filter { row ->
            requiredPermissions == null || requiredPermissions.any { PermissionSnapshot.normalizeToken(it) in row.tokens }
        }
    val broken = rows.filter { !it.granted }
    val locationOff = state.permissions?.locationServiceOff(requiredPermissions ?: rows.flatMap { it.tokens }) == true

    PermissionRepairContent(
        rows = rows,
        broken = broken,
        locationOff = locationOff,
        onOpenLocationSettings = {
            runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        },
        onIntent = viewModel::onIntent,
        onFix = { row -> requestFix(row, context, runtimeLauncher, healthLauncher, healthAvailable) },
        modifier = modifier,
    )
}

/** 권한 재연결 화면 본문. */
@Composable
internal fun PermissionRepairContent(
    rows: List<RepairRow>,
    broken: List<RepairRow>,
    onIntent: (PermissionRepairIntent) -> Unit,
    onFix: (RepairRow) -> Unit,
    modifier: Modifier = Modifier,
    locationOff: Boolean = false,
    onOpenLocationSettings: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "인증 연결 끊김", onBack = { onIntent(PermissionRepairIntent.Back) })

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .ttiContentDrawn(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (broken.isNotEmpty()) {
                // 권한 복구 기한 안내.
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RuleUpTheme.shapes.medium)
                            .background(RuleUpTheme.colors.dangerContainer)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "오늘 밤 12시까지 다시 허용하지 않으면 챌린지에서 나가게 돼요",
                        color = RuleUpTheme.colors.danger,
                        style = RuleUpTheme.typography.cardTitle,
                    )
                    Text(
                        text = "그동안 인증이 되지 않아 실패로 기록될 수 있어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.caption,
                    )
                }
            }

            if (locationOff) {
                // 권한과 달리 기기 설정에서 켜야 해서 권한 목록과 따로 둔다.
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RuleUpTheme.shapes.medium)
                            .background(RuleUpTheme.colors.dangerContainer)
                            .singleClickable(onClick = onOpenLocationSettings)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "휴대폰 위치(GPS)가 꺼져 있어요",
                        color = RuleUpTheme.colors.danger,
                        style = RuleUpTheme.typography.cardTitle,
                    )
                    Text(
                        text = "권한을 허용해도 위치가 꺼져 있으면 장소 인증이 되지 않아요 · 눌러서 위치 켜기",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.caption,
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.medium)
                        .background(RuleUpTheme.colors.surface)
                        .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.medium),
            ) {
                rows.forEach { row ->
                    PermissionStatusRow(
                        row = row,
                        onFix = { onFix(row) },
                    )
                }
            }
        }
    }
}

/** 권한별 설정 화면. */
private fun requestFix(
    row: RepairRow,
    context: android.content.Context,
    runtimeLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    healthLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>,
    healthAvailable: Boolean,
) {
    when (row.kind) {
        PermissionRequestKind.RUNTIME ->
            row.runtimePermissions
                .takeIf { it.isNotEmpty() }
                ?.let { runtimeLauncher.launch(it.toTypedArray()) }

        PermissionRequestKind.USAGE_ACCESS_SETTINGS ->
            runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                .onFailure {
                    android.widget.Toast
                        .makeText(context, "설정 화면을 열 수 없어요", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }.let { }

        PermissionRequestKind.HEALTH_CONNECT ->
            if (healthAvailable) {
                runCatching {
                    healthLauncher.launch(
                        healthReadPermissions()
                            .filter { permission ->
                                row.tokens.any { permission.endsWith(it) }
                            }.toSet(),
                    )
                }.onFailure {
                    android.widget.Toast
                        .makeText(context, "권한 화면을 열 수 없어요", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                android.widget.Toast
                    .makeText(context, "이 기기에서는 헬스 커넥트를 사용할 수 없어요", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
    }
}

@Composable
private fun PermissionStatusRow(
    row: RepairRow,
    onFix: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = row.label, color = colors.textPrimary, style = RuleUpTheme.typography.bodyBold)
            Text(
                text = if (row.granted) "연결돼 있어요" else "꺼짐 · ${row.purpose}",
                color = if (row.granted) colors.success else colors.danger,
                style = RuleUpTheme.typography.caption,
            )
        }
        if (!row.granted) {
            Text(
                text = if (row.kind == PermissionRequestKind.RUNTIME) "허용" else "설정",
                color = colors.brand,
                style = RuleUpTheme.typography.bodyBold,
                modifier = Modifier.singleClickable(onClick = onFix).padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** 화면에 한 줄로 서는 신호. */
internal data class RepairRow(
    val label: String,
    val purpose: String,
    val granted: Boolean,
    val kind: PermissionRequestKind,
    val runtimePermissions: List<String> = emptyList(),
    val tokens: Set<String> = emptySet(),
)

/** 스냅샷 → 화면 줄. */
internal fun repairRows(snapshot: PermissionSnapshot): List<RepairRow> =
    buildList {
        add(
            RepairRow(
                label = "위치",
                tokens = setOf("LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE"),
                purpose = "등록한 장소 도착 확인에 필요",
                granted = snapshot.location == PermissionState.GRANTED,
                kind = PermissionRequestKind.RUNTIME,
                runtimePermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            ),
        )
        add(
            RepairRow(
                label = "백그라운드 위치",
                tokens = setOf("ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION"),
                purpose = "앱을 열지 않아도 도착을 확인하려면 필요",
                granted = snapshot.backgroundLocation == PermissionState.GRANTED,
                kind = PermissionRequestKind.RUNTIME,
                // 다이얼로그로 한 번에 받을 수 없어 OS 가 설정 화면으로 안내한다.
                runtimePermissions =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        emptyList()
                    },
            ),
        )
        add(
            RepairRow(
                label = "사용 정보 접근",
                tokens = setOf("PACKAGE_USAGE_STATS", "USAGE_STATS", "SCREEN_TIME"),
                purpose = "앱 사용 시간·기상 확인에 필요",
                granted = snapshot.usageStats == PermissionState.GRANTED,
                kind = PermissionRequestKind.USAGE_ACCESS_SETTINGS,
            ),
        )
        add(
            RepairRow(
                label = "걸음",
                tokens = setOf("READ_STEPS", "HEALTH_STEPS", "HEALTH"),
                purpose = "헬스 커넥트에서 읽어요",
                granted = snapshot.healthSteps == PermissionState.GRANTED,
                kind = PermissionRequestKind.HEALTH_CONNECT,
            ),
        )
        add(
            RepairRow(
                label = "거리",
                purpose = "헬스 커넥트에서 읽어요",
                granted = snapshot.healthDistance == PermissionState.GRANTED,
                kind = PermissionRequestKind.HEALTH_CONNECT,
                tokens = setOf("READ_DISTANCE", "HEALTH_DISTANCE"),
            ),
        )
        add(
            RepairRow(
                label = "수면",
                tokens = setOf("READ_SLEEP", "HEALTH_SLEEP", "SLEEP"),
                purpose = "헬스 커넥트에서 읽어요",
                granted = snapshot.healthSleep == PermissionState.GRANTED,
                kind = PermissionRequestKind.HEALTH_CONNECT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                RepairRow(
                    label = "알림",
                    tokens = setOf("POST_NOTIFICATIONS", "NOTIFICATION"),
                    purpose = "판정 결과·권한 복구 안내에 필요",
                    granted = snapshot.postNotifications == PermissionState.GRANTED,
                    kind = PermissionRequestKind.RUNTIME,
                    runtimePermissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
                ),
            )
        }
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun PermissionRepairContentPreview() {
    RuleUpTheme {
        PermissionRepairContent(rows = emptyList(), broken = emptyList(), onIntent = { }, onFix = { })
    }
}
