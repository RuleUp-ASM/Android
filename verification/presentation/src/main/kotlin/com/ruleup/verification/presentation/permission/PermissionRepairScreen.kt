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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.permission.healthConnectAvailable
import com.ruleup.ui.permission.healthReadPermissions
import com.ruleup.ui.permission.rememberHealthPermissionLauncher
import com.ruleup.verification.domain.entity.PermissionRequestKind
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.presentation.permission.viewmodel.PermissionRepairIntent
import com.ruleup.verification.presentation.permission.viewmodel.PermissionRepairViewModel

/**
 * 권한 재연결(Figma `1134:997`). 권한이 끊기면 인증이 조용히 멈추고, 사용자는 실패가 쌓이는 이유를
 * 모른 채 강퇴까지 간다. 끊긴 것만이 아니라 살아 있는 신호도 함께 세워야 원인이 좁혀진다.
 */
@Composable
fun PermissionRepairScreen(
    modifier: Modifier = Modifier,
    viewModel: PermissionRepairViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 설정에서 켜고 돌아오는 것이 이 화면의 주된 동선이다 — 돌아올 때마다 다시 읽는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(PermissionRepairIntent.Refresh)
    }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.onIntent(PermissionRepairIntent.Refresh)
        }
    // 걸음·수면은 HC 자체 권한 화면으로 — 위치가 OS 다이얼로그를 직접 여는 것과 같은 방식이다.
    val healthLauncher = rememberHealthPermissionLauncher { viewModel.onIntent(PermissionRepairIntent.Refresh) }
    val healthAvailable = healthConnectAvailable()

    val rows = state.permissions?.let(::repairRows).orEmpty()
    val broken = rows.filter { !it.granted }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "인증 연결 끊김", onBack = { viewModel.onIntent(PermissionRepairIntent.Back) })

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (broken.isNotEmpty()) {
                // 강퇴로 이어진다는 사실을 시점과 함께 말한다 — "권한이 필요해요"로는 급한 줄 모른다.
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
                        onFix = {
                            when (row.kind) {
                                PermissionRequestKind.RUNTIME ->
                                    row.runtimePermissions
                                        .takeIf { it.isNotEmpty() }
                                        ?.let { runtimeLauncher.launch(it.toTypedArray()) }

                                PermissionRequestKind.USAGE_ACCESS_SETTINGS ->
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

                                // 미지원 기기는 요청 화면이 뜨지 않는다 — 앱 정보로 보내 사용자가
                                // 헬스 커넥트 설치·연결을 확인하게 한다.
                                PermissionRequestKind.HEALTH_CONNECT ->
                                    if (healthAvailable) {
                                        healthLauncher.launch(healthReadPermissions())
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                                    }
                            }
                        },
                    )
                }
            }
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

/** 화면에 한 줄로 서는 신호. [kind] 가 "어느 문을 열어야 하는지"를 정한다. */
internal data class RepairRow(
    val label: String,
    val purpose: String,
    val granted: Boolean,
    val kind: PermissionRequestKind,
    val runtimePermissions: List<String> = emptyList(),
)

/** 스냅샷 → 화면 줄. **끊긴 것만 거르지 않는다** — 살아 있는 신호를 함께 보여야 원인을 좁힌다. */
internal fun repairRows(snapshot: PermissionSnapshot): List<RepairRow> =
    buildList {
        add(
            RepairRow(
                label = "위치",
                purpose = "등록한 장소 도착 확인에 필요",
                granted = snapshot.location == PermissionState.GRANTED,
                kind = PermissionRequestKind.RUNTIME,
                runtimePermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            ),
        )
        add(
            RepairRow(
                label = "백그라운드 위치",
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
                purpose = "앱 사용 시간·기상 확인에 필요",
                granted = snapshot.usageStats == PermissionState.GRANTED,
                kind = PermissionRequestKind.USAGE_ACCESS_SETTINGS,
            ),
        )
        add(
            RepairRow(
                label = "걸음·거리",
                purpose = "헬스 커넥트에서 읽어요",
                granted = snapshot.healthSteps == PermissionState.GRANTED,
                kind = PermissionRequestKind.HEALTH_CONNECT,
            ),
        )
        add(
            RepairRow(
                label = "수면",
                purpose = "헬스 커넥트에서 읽어요",
                granted = snapshot.healthSleep == PermissionState.GRANTED,
                kind = PermissionRequestKind.HEALTH_CONNECT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                RepairRow(
                    label = "알림",
                    purpose = "판정 결과·권한 복구 안내에 필요",
                    granted = snapshot.postNotifications == PermissionState.GRANTED,
                    kind = PermissionRequestKind.RUNTIME,
                    runtimePermissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
                ),
            )
        }
    }
