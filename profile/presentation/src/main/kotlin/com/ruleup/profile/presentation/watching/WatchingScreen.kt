package com.ruleup.profile.presentation.watching

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.presentation.watching.viewmodel.WatchingEffect
import com.ruleup.profile.presentation.watching.viewmodel.WatchingIntent
import com.ruleup.profile.presentation.watching.viewmodel.WatchingState
import com.ruleup.profile.presentation.watching.viewmodel.WatchingViewModel
import com.ruleup.ui.helper.LocalMessageHelper

/**
 * 패널티 수신 관리 — 「내가 받는 알림」 (Figma 1134:2221).
 *
 * 이 화면에는 **관계를 끊는 버튼이 없다** — 정책상 감시자 해제가 폐지됐고, 관계는 루틴이 끝나면
 * 배치가 지운다. 사용자가 할 수 있는 건 푸시를 끄는 것(행 토글)과 완전 수신거부(행 탭)뿐이다.
 */
@Composable
fun WatchingScreen(
    modifier: Modifier = Modifier,
    viewModel: WatchingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) { viewModel.onIntent(WatchingIntent.Load) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WatchingEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    WatchingContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchingContent(
    state: WatchingState,
    onIntent: (WatchingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "내가 받는 알림", onBack = { onIntent(WatchingIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.errorMessage != null && state.items.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage,
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            state.items.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "아직 감시자로 지정된 곳이 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> WatchingList(state = state, onIntent = onIntent)
        }
    }

    state.revokeTarget?.let {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(WatchingIntent.DismissRevoke) },
            sheetState = sheetState,
            containerColor = RuleUpTheme.colors.surface,
        ) {
            RevokeSheet(isSubmitting = state.updating != null, onIntent = onIntent)
        }
    }
}

@Composable
private fun WatchingList(
    state: WatchingState,
    onIntent: (WatchingIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "이 사람들이 실패한 날, 나에게 알림이 와요",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.small,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        items(state.items, key = { it.watcherId }) { item ->
            WatchingRow(
                item = item,
                enabled = state.updating == null,
                onToggle = { onIntent(WatchingIntent.TogglePush(item.watcherId, it)) },
                onRevoke = { onIntent(WatchingIntent.ConfirmRevoke(item.watcherId)) },
            )
        }
        item {
            Text(
                text = "끄면 푸시만 멈추고 알림함에는 남아요 · 행을 탭하면 아예 받지 않을 수 있어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun WatchingRow(
    item: Watching,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onRevoke: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .singleClickable(onClick = onRevoke)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.ownerNickname.take(1),
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.ownerNickname,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(
                text = item.challengeTitle,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Switch(
            checked = item.pushEnabled,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = RuleUpTheme.colors.brand),
        )
    }
}

/**
 * 완전 수신거부 확인.
 *
 * **되돌릴 수 없다는 사실**을 먼저 말한다 — 되살리는 경로가 없고, 같은 사람이 30일간 다시 지정하지도
 * 못한다. 푸시만 끄고 싶은 사용자가 여기까지 오면 안 된다.
 */
@Composable
private fun RevokeSheet(
    isSubmitting: Boolean,
    onIntent: (WatchingIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
    ) {
        Text(
            text = "이 알림을 아예 받지 않을까요?",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "다시 켤 수 없어요. 이 사람은 30일 동안 나를 감시자로 다시 지정할 수 없어요. 잠깐만 멈추려면 토글을 꺼 주세요.",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
        Spacer(Modifier.height(20.dp))
        RuleUpPrimaryButton(
            text = "받지 않을게요",
            enabled = !isSubmitting,
            onClick = { onIntent(WatchingIntent.Revoke) },
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .singleClickable(onClick = { onIntent(WatchingIntent.DismissRevoke) })
                    .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "닫기",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}
