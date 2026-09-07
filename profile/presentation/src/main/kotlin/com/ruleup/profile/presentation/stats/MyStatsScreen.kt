package com.ruleup.profile.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.CycleResult
import com.ruleup.profile.domain.entity.CycleWeek
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsIntent
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsState
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsViewModel

/**
 * 통계 리포트. 정책이 정한 **지표 5종 고정** — 전체 성공률 · 총 성공 인증 수 · 연속 성공 ·
 * 최근 12주 사이클 · 완주 개수(명세: GET /me/stats).
 *
 * 기간 탭(주간/월간/연간)은 없다 — 명세에서 폐기됐다.
 */
@Composable
fun MyStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyStatsIntent.Load)
    }

    MyStatsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "통계를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
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
        CyclesCard(cycles = report.cycles12w)
    }
}

/** 전체 성공률. 판정 이력이 없으면 0% 대신 비어 있다고 말한다 — 둘은 다른 사실이다. */
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

/** 최근 12주 그리드. 판정이 없던 주는 회색 — 실패와 같은 색으로 칠하면 없던 실패를 새기게 된다. */
@Composable
private fun CyclesCard(cycles: List<CycleWeek>) {
    StatsCard {
        Text(
            text = "최근 12주",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.smallBold,
        )
        if (cycles.isEmpty()) {
            Text(
                text = "아직 지나간 주가 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
            )
            return@StatsCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            cycles.forEach { cycle ->
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cycle.result.cellColor),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(color = CycleResult.SUCCESS.cellColor, label = "성공")
            LegendDot(color = CycleResult.PARTIAL.cellColor, label = "일부")
            LegendDot(color = CycleResult.FAIL.cellColor, label = "실패")
            LegendDot(color = CycleResult.NONE.cellColor, label = "판정 없음")
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
                    .padding(5.dp),
        )
        Text(
            text = label,
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.micro,
        )
    }
}

private val CycleResult?.cellColor: Color
    @Composable
    get() =
        when (this) {
            CycleResult.SUCCESS -> RuleUpTheme.colors.success
            CycleResult.PARTIAL -> RuleUpTheme.colors.warning
            CycleResult.FAIL -> RuleUpTheme.colors.danger
            // 판정이 없던 주와 모르는 값은 같은 회색 — 둘 다 "결과라고 말할 게 없다"는 뜻이다.
            CycleResult.NONE, null -> RuleUpTheme.colors.border
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
