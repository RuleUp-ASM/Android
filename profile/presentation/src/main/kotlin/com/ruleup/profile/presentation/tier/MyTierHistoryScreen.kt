package com.ruleup.profile.presentation.tier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.TierBest
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.entity.TierSnapshot
import com.ruleup.profile.presentation.common.accentColor
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.common.label
import com.ruleup.profile.presentation.common.thousandsLabel
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryIntent
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryState
import com.ruleup.profile.presentation.tier.viewmodel.MyTierHistoryViewModel

/**
 * 티어 히스토리. 월말 스냅샷 시리즈와 역대 최고만 그린다.
 *
 * 하락 사유는 그리지 않는다 — 정책이 "그래프 형식 + 하락 사유 표기 없음"으로 정해 서버도
 * 사유를 내려주지 않는다(명세: GET /me/tier/history).
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

            state.history == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "히스토리를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> HistoryBody(history = state.history)
        }
    }
}

@Composable
private fun HistoryBody(history: TierHistory) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        history.best?.let { item { BestCard(best = it) } }
        item { MonthlyChart(monthly = history.monthly) }
        item {
            Text(
                text = "월말 기록",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallBold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (history.monthly.isEmpty()) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 쌓인 기록이 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.small,
                    )
                }
            }
        } else {
            items(history.monthly.reversed(), key = { it.month }) { MonthlyRow(snapshot = it) }
        }
        history.retentionNote?.let {
            item {
                Text(
                    text = "$it — 그 이전 기록은 지워져요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
            }
        }
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

@Composable
private fun MonthlyRow(snapshot: TierSnapshot) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = snapshot.month.replace('-', '.'),
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = snapshot.endTier.label,
            color = snapshot.endTier.accentColor,
            style = RuleUpTheme.typography.smallBold,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = "${snapshot.endScore.thousandsLabel()}점",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}
