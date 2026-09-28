package com.ruleup.profile.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsIntent
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsState
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsViewModel
import com.ruleup.tti.presentation.TtiScreenEffect

/** 통계 리포트. */
@Composable
fun MyStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyStatsIntent.Load)
    }

    MyStatsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun MyStatsContent(
    state: MyStatsState,
    onIntent: (MyStatsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "통계", onBack = { onIntent(MyStatsIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.report == null ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "통계를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { onIntent(MyStatsIntent.Load) }) { Text("다시 시도") }
                }

            else -> StatsBody(report = state.report)
        }
    }
}

@Composable
private fun StatsBody(report: StatsReport) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SuccessRateCard(rate = report.successRate)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                label = "총 성공 인증",
                value = "${report.totalSuccessCount}",
                unit = "회",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "완주",
                value = "${report.completedCount}",
                unit = "개",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                label = "연속 성공",
                value = "${report.streak.current}",
                unit = "일",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "최고 연속",
                value = "${report.streak.best}",
                unit = "일",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 전체 성공률. */
@Composable
private fun SuccessRateCard(rate: Double?) {
    StatsCard {
        Text(
            text = "전체 성공률",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.smallBold,
        )
        if (rate == null) {
            Text(
                text = "아직 판정된 인증이 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${(rate * 100).toInt()}",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.numberXl,
                )
                Text(
                    text = "%",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.smallBold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    StatsCard(modifier = modifier) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.numberM,
            )
            Text(
                text = unit,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MyStatsContentPreview() {
    RuleUpTheme {
        MyStatsContent(
            state =
                com.ruleup.profile.presentation.stats.viewmodel.MyStatsState.initial
                    .copy(isLoading = false),
            onIntent = { },
        )
    }
}
