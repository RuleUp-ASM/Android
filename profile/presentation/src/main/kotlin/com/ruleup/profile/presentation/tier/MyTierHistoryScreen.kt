package com.ruleup.profile.presentation.tier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.TierBest
import com.ruleup.profile.domain.entity.TierSnapshot
import com.ruleup.profile.presentation.common.accentColor
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.common.deltaLabel
import com.ruleup.profile.presentation.common.label
import com.ruleup.profile.presentation.common.thousandsLabel
import com.ruleup.profile.presentation.common.titleLabel
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryIntent
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryState
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryViewModel

/**
 * 티어 히스토리. 위는 **그래프**(월말 스냅샷 · `/me/tier/history`), 아래는 **점수 변동 이력**
 * (`/me/tier/changes` · 커서 50건씩)이다.
 *
 * 그래프에 하락 사유를 표기하지 않는 것이 정책이고 서버도 스냅샷에는 사유를 싣지 않는다. 그
 * 제한은 2026-09-07 개정으로 **그래프 한정**이 됐으므로 아래 이력에는 사유를 그대로 쓴다.
 */
@Composable
fun MyTierHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: MyTierHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyTierHistoryIntent.Load)
    }

    MyTierHistoryContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun MyTierHistoryContent(
    state: MyTierHistoryState,
    onIntent: (MyTierHistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "티어 히스토리", onBack = { onIntent(MyTierHistoryIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.history == null && state.changes.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "히스토리를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> HistoryBody(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun HistoryBody(
    state: MyTierHistoryState,
    onIntent: (MyTierHistoryIntent) -> Unit,
) {
    val loadMore by rememberUpdatedState { onIntent(MyTierHistoryIntent.LoadMore) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.history?.best?.let { item { BestCard(best = it) } }
        state.history?.let { item { MonthlyChart(monthly = it.monthly) } }
        item {
            Text(
                text = "점수 변동",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallBold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (state.changes.isEmpty()) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 점수가 움직인 적이 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.small,
                    )
                }
            }
        } else {
            // 같은 날 같은 방에서 두 건이 날 수 있어 date 만으로는 키가 겹친다 — 위치를 섞는다.
            itemsIndexed(state.changes) { index, change ->
                ChangeRow(change = change)
                // 마지막 줄이 보이면 다음 페이지를 당긴다. 커서가 없으면 VM 이 무시한다.
                if (index == state.changes.lastIndex && state.canLoadMore) {
                    LaunchedEffect(state.nextCursor) { loadMore() }
                }
            }
        }
        state.retentionDays?.let { days ->
            item {
                Text(
                    text = "${days}일치까지 보관해요 — 그 이전 기록은 지워져요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
            }
        }
    }
}

/** 「아침 6:30 기상 · 사이클 성공 · 9.7 · +8」. 챌린지명이 없으면 사유만 남는다. */
@Composable
private fun ChangeRow(change: ScoreChange) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = change.titleLabel,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.small,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = dateDotLabel(change.date),
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = change.deltaLabel,
            color = if (change.delta < 0) RuleUpTheme.colors.danger else RuleUpTheme.colors.success,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}

@Composable
private fun BestCard(best: TierBest) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(RuleUpGradients.Brand)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "역대 최고 ${best.tier.label}",
            color = RuleUpPalette.BgSurface,
            style = RuleUpTheme.typography.smallBold,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${best.score.thousandsLabel()}점",
                color = RuleUpPalette.BgSurface,
                style = RuleUpTheme.typography.numberXl,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = dateDotLabel(best.date),
                color = RuleUpPalette.BgSurface.copy(alpha = 0.85f),
                style = RuleUpTheme.typography.small,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
    }
}

/**
 * 월말 점수 막대. 축은 **0~2,000 고정**이다 — 최댓값에 맞춰 늘이면 달마다 같은 높이가 다른
 * 점수를 뜻하게 되어 그래프가 거짓말을 한다.
 */
@Composable
private fun MonthlyChart(monthly: List<TierSnapshot>) {
    if (monthly.isEmpty()) return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(16.dp)
                .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        monthly.forEach { snapshot ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ratio = (snapshot.endScore.toFloat() / Tier.RUBY.maxScore).coerceIn(0.02f, 1f)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(snapshot.endTier.accentColor),
                )
                Text(
                    text = snapshot.month.takeLast(2),
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.micro,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
