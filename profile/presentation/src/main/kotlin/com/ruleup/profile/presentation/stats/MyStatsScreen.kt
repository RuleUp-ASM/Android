package com.ruleup.profile.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsPoint
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.presentation.common.trimLabel
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsIntent
import com.ruleup.profile.presentation.stats.viewmodel.MyStatsViewModel

// 막대 그라데이션 (피그마 435:250 — violet 계열)
private val BarGradient = listOf(RuleUpPalette.Primary300, RuleUpPalette.Primary600)

/** 통계 리포트 (피그마 435:250). 주간/월간/연간 탭 + 지표 4카드 + 완주율 시리즈 + 인사이트. */
@Composable
fun MyStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyStatsIntent.Load)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "통계", onBack = { viewModel.onIntent(MyStatsIntent.Back) })

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PeriodTabs(
                selected = state.period,
                onSelect = { viewModel.onIntent(MyStatsIntent.SelectPeriod(it)) },
            )

            when {
                state.isLoading ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                    }

                state.report == null ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.errorMessage ?: "통계를 불러오지 못했어요",
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }

                else -> StatsBody(report = state.report!!)
            }
        }
    }
}

@Composable
private fun PeriodTabs(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(4.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) RuleUpTheme.colors.surface else Color.Transparent)
                        .singleClickable(onClick = { onSelect(period) })
                        .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    text = period.label,
                    color = if (isSelected) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textSecondary,
                    style = if (isSelected) RuleUpTheme.typography.smallBold else RuleUpTheme.typography.smallMedium,
                )
            }
        }
    }
}

@Composable
private fun StatsBody(report: StatsReport) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val mannerLabel =
            when {
                report.mannerDelta > 0 -> "+${report.mannerDelta.trimLabel()}℃"
                report.mannerDelta < 0 -> "−${(-report.mannerDelta).trimLabel()}℃"
                else -> "0℃"
            }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                value = "${report.totalCompleted}",
                label = "총 완주",
                valueColor = RuleUpTheme.colors.success,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "${report.avgCompletionRate}%",
                label = "평균 완주율",
                valueColor = RuleUpTheme.colors.brand,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                value = mannerLabel,
                label = "매너 상승",
                valueColor = RuleUpPalette.StatusWarn,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "${report.avgStreak.trimLabel()}일",
                label = "평균 연속",
                valueColor = RuleUpPalette.Primary300,
                modifier = Modifier.weight(1f),
            )
        }

        SeriesChartCard(period = report.period, series = report.series)

        report.insight?.let { InsightBanner(insight = it) }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            color = valueColor,
            style = RuleUpTheme.typography.title,
        )
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.captionMedium,
        )
    }
}

/**
 * 완주율 시리즈 바 차트 (시안 435:250 — 막대별 값·bucket 라벨).
 * 시안이 막대마다 값 라벨을 붙이는 구조라 차트 라이브러리 대신 직접 그린다.
 */
@Composable
private fun SeriesChartCard(
    period: StatsPeriod,
    series: List<StatsPoint>,
) {
    val title =
        when (period) {
            StatsPeriod.WEEKLY -> "일별 완주율"
            StatsPeriod.MONTHLY -> "주간 완주율"
            StatsPeriod.YEARLY -> "월별 완주율"
        }
    RuleUpCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        if (series.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "아직 집계된 기록이 없어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.small,
                )
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                series.forEach { point ->
                    SeriesBar(point = point, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val BAR_MAX_HEIGHT_DP = 96

@Composable
private fun SeriesBar(
    point: StatsPoint,
    modifier: Modifier = Modifier,
) {
    // 0% 도 존재를 알 수 있게 최소 높이를 준다.
    val barHeight = (BAR_MAX_HEIGHT_DP * point.completionRate / 100).coerceAtLeast(4).dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(22.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(Brush.verticalGradient(BarGradient)),
        )
        Text(
            text = "${point.completionRate}",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.tinyBold,
        )
        Text(
            text = point.bucket.shortBucket(),
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.micro,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 주간 bucket 은 날짜(YYYY-MM-DD)로 오므로 요일로 줄인다. W1·1월 등은 그대로.
private fun String.shortBucket(): String {
    val parsed = runCatching { java.time.LocalDate.parse(this) }.getOrNull() ?: return this
    return parsed.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.KOREAN)
}

@Composable
private fun InsightBanner(insight: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(RuleUpPalette.Primary50, Color(0xFFFCE7F3))),
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🤖", style = RuleUpTheme.typography.labelMedium)
        Spacer(Modifier.width(10.dp))
        Text(
            text = insight,
            color = RuleUpPalette.TextSub,
            style = RuleUpTheme.typography.smallMedium,
        )
    }
}
