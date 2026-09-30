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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.presentation.watching.viewmodel.WatchingEffect
import com.ruleup.profile.presentation.watching.viewmodel.WatchingIntent
import com.ruleup.profile.presentation.watching.viewmodel.WatchingState
import com.ruleup.profile.presentation.watching.viewmodel.WatchingViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.helper.LocalMessageHelper

/** 패널티 수신 관리 */
@Composable
fun WatchingScreen(
    modifier: Modifier = Modifier,
    viewModel: WatchingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
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

/** 화면 본문. */
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
                Column(
                    Modifier.fillMaxSize().ttiContentDrawn(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage,
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { onIntent(WatchingIntent.Load) }) { Text("다시 시도") }
                }

            state.items.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(horizontal = 40.dp).ttiContentDrawn(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "아직 감시자로 지정된 곳이 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> WatchingList(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun WatchingList(
    state: WatchingState,
    onIntent: (WatchingIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().ttiContentDrawn(),
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
            WatchingRow(item = item)
        }
        item {
            Text(
                text = "푸시는 알림 설정에서 켜고 끌 수 있어요 · 알림함에는 그대로 남아요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun WatchingRow(item: Watching) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
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
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun WatchingContentPreview() {
    RuleUpTheme {
        WatchingContent(
            state =
                com.ruleup.profile.presentation.watching.viewmodel.WatchingState.initial
                    .copy(isLoading = false),
            onIntent = { },
        )
    }
}
