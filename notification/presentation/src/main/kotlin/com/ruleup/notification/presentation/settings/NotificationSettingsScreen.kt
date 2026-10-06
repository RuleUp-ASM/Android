package com.ruleup.notification.presentation.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.presentation.settings.viewmodel.NotificationSettingsEffect
import com.ruleup.notification.presentation.settings.viewmodel.NotificationSettingsIntent
import com.ruleup.notification.presentation.settings.viewmodel.NotificationSettingsState
import com.ruleup.notification.presentation.settings.viewmodel.NotificationSettingsViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.helper.LocalMessageHelper

/** 알림 설정. */
@Composable
fun NotificationSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    val messageHelper = LocalMessageHelper.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onIntent(NotificationSettingsIntent.Load) }

    // OS 설정에서 돌아왔을 수 있다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onPermissionChecked(!NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationSettingsEffect.ShowMessage -> messageHelper.showToast(effect.message)

                NotificationSettingsEffect.OpenSystemSettings -> {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData("package:${context.packageName}".toUri())
                        }
                    runCatching { context.startActivity(intent) }
                        .onFailure { messageHelper.showToast("설정 화면을 열지 못했어요") }
                }
            }
        }
    }

    NotificationSettingsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun NotificationSettingsContent(
    state: NotificationSettingsState,
    onIntent: (NotificationSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "알림 설정", onBack = { onIntent(NotificationSettingsIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.settings == null ->
                Column(
                    Modifier.fillMaxSize().ttiContentDrawn(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "알림 설정을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { onIntent(NotificationSettingsIntent.Load) }) { Text("다시 시도") }
                }

            else -> SettingsBody(state = state, settings = state.settings, onIntent = onIntent)
        }
    }
}

@Composable
private fun SettingsBody(
    state: NotificationSettingsState,
    settings: NotificationSettings,
    onIntent: (NotificationSettingsIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
                .ttiContentDrawn(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.systemPermissionDenied) {
            PermissionBanner(onClick = { onIntent(NotificationSettingsIntent.OpenSystemSettings) })
        }

        SettingsCard {
            ToggleRow(
                label = "푸시 알림",
                note = "끄면 리마인더까지 모든 푸시가 멈춰요",
                checked = settings.pushEnabled,
                // 화면이 먼저 바뀌므로 요청 중에 회색으로 잠그지 않는다(겹친 탭은 ViewModel 이 무시한다)
                enabled = true,
                onToggle = { onIntent(NotificationSettingsIntent.ToggleMaster(it)) },
            )
        }

        SectionLabel("종류")
        SettingsCard {
            GroupRow(
                group = NotificationGroup.ACCOUNT,
                label = "계정",
                note = "강퇴 · 잠금 · 심사 결과 · 이의 결과",
                state = state,
                settings = settings,
                onIntent = onIntent,
            )
            HorizontalDivider(color = RuleUpTheme.colors.border)
            GroupRow(
                group = NotificationGroup.CHALLENGE,
                label = "챌린지",
                note = "판정 결과 · 시작/종료 · 티어 · 감시자",
                state = state,
                settings = settings,
                onIntent = onIntent,
            )
            HorizontalDivider(color = RuleUpTheme.colors.border)
            GroupRow(
                group = NotificationGroup.MARKETING,
                label = "마케팅 정보 수신",
                note = "끄면 광고성 정보 수신 동의도 함께 철회돼요",
                state = state,
                settings = settings,
                onIntent = onIntent,
            )
        }

        if (settings.mutedChallengeIds.isNotEmpty()) {
            SectionLabel("음소거 중인 챌린지")
            SettingsCard {
                Text(
                    text = "${settings.mutedChallengeIds.size}개 챌린지의 알림을 끄고 있어요. 켜고 끄기는 각 챌린지 방에서 해요 — 여기서는 방 이름을 알 수 없어요.",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.small,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** OS 권한이 꺼졌을 때. */
@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.warningContainer)
                .singleClickable(onClick = onClick)
                .padding(16.dp),
    ) {
        Text(
            text = "휴대폰에서 알림이 꺼져 있어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "아래 설정을 켜도 알림이 오지 않아요. 눌러서 휴대폰 설정에서 허용해 주세요.",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
    }
}

@Composable
private fun GroupRow(
    group: NotificationGroup,
    label: String,
    note: String,
    state: NotificationSettingsState,
    settings: NotificationSettings,
    onIntent: (NotificationSettingsIntent) -> Unit,
) {
    ToggleRow(
        label = label,
        // 마스터가 꺼져 있으면 이 토글이 켜져 있어도 푸시가 안 나간다
        note = if (settings.pushEnabled) note else "푸시 알림이 꺼져 있어 지금은 오지 않아요",
        checked = settings.groups.of(group),
        enabled = settings.pushEnabled,
        onToggle = { onIntent(NotificationSettingsIntent.ToggleGroup(group, it)) },
    )
}

@Composable
private fun ToggleRow(
    label: String,
    note: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (enabled) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(text = note, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = RuleUpTheme.colors.brand),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.captionBold,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NotificationSettingsContentPreview() {
    RuleUpTheme {
        NotificationSettingsContent(
            state =
                com.ruleup.notification.presentation.settings.viewmodel.NotificationSettingsState.initial.copy(
                    isLoading = false,
                ),
            onIntent = {
            },
        )
    }
}
