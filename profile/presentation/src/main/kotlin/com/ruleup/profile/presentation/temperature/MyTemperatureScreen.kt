package com.ruleup.profile.presentation.temperature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.NextTier
import com.ruleup.profile.domain.entity.ReputationChange
import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.common.trimLabel
import com.ruleup.profile.presentation.temperature.viewmodel.MyTemperatureIntent
import com.ruleup.profile.presentation.temperature.viewmodel.MyTemperatureViewModel

// 온도 히어로/진행 바 그라데이션 (피그마 434:311 — amber → rose)
private val TemperatureGradient = listOf(Color(0xFFF59E0B), Color(0xFFF43F5E))

/** 매너 온도 상세 (피그마 434:311). 현재 온도·다음 목표 진행·최근 변동 10건. */
@Composable
fun MyTemperatureScreen(
    modifier: Modifier = Modifier,
    viewModel: MyTemperatureViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyTemperatureIntent.Load)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "매너 온도", onBack = { viewModel.onIntent(MyTemperatureIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.detail == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "온도 정보를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else ->
                TemperatureBody(
                    detail = state.detail!!,
                    onOpenHistory = { viewModel.onIntent(MyTemperatureIntent.OpenHistory) },
                )
        }
    }
}

@Composable
private fun TemperatureBody(
    detail: ReputationDetail,
    onOpenHistory: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TemperatureHero(detail = detail)
        detail.nextTier?.let { NextTierCard(current = detail.current, nextTier = it) }
        detail.nextTier
            ?.label
            ?.takeIf { it.isNotBlank() }
            ?.let { TierBenefitBanner(label = it) }
        HistoryLinkRow(onClick = onOpenHistory)
        RecentChangesCard(changes = detail.recentChanges)
    }
}

@Composable
private fun TemperatureHero(detail: ReputationDetail) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(TemperatureGradient))
                .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 장식용 글리프라 타입 스케일(최대 22)에 넣으면 확 줄어든다. 그리는 크기로 잡는다.
        Text(text = "🌡️", fontSize = 26.sp)
        Text(
            text = "${detail.current.trimLabel()}℃",
            color = RuleUpPalette.White,
            style = RuleUpTheme.typography.numberXl,
        )
        if (detail.bandLabel.isNotBlank()) {
            Text(
                text = detail.bandLabel,
                color = RuleUpPalette.White,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
    }
}

/** 다음 앵커 진행 바: (현재 − 이전 앵커) ÷ (target − 이전 앵커)를 서버가 progressRate 로 내려준다. */
@Composable
private fun NextTierCard(
    current: Double,
    nextTier: NextTier,
) {
    val remaining = (nextTier.target - current).coerceAtLeast(0.0)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row {
            Text(
                text = "다음 레벨까지 ",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
            )
            Text(
                text = "${nextTier.target.trimLabel()}℃",
                color = RuleUpPalette.Amber500,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RuleUpTheme.colors.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(nextTier.progressRate.toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(TemperatureGradient)),
            )
        }
        Row {
            Text(
                text = "${current.trimLabel()} / ${nextTier.target.trimLabel()}℃ · ",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
            )
            Text(
                text = "${remaining.trimLabel()}℃ 남음",
                color = RuleUpPalette.Amber500,
                style = RuleUpTheme.typography.smallBold,
            )
        }
    }
}

@Composable
private fun TierBenefitBanner(label: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.brandSoft)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "✨", style = RuleUpTheme.typography.labelMedium)
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}

@Composable
private fun HistoryLinkRow(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .singleClickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🏆", style = RuleUpTheme.typography.labelMedium)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "평판 히스토리",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "›",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.section,
        )
    }
}

@Composable
private fun RecentChangesCard(changes: List<ReputationChange>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "최근 변동",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.smallBold,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp)),
        ) {
            if (changes.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 변동 기록이 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.small,
                    )
                }
            } else {
                changes.forEachIndexed { index, change ->
                    if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
                    ChangeRow(change = change)
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: ReputationChange) {
    val deltaColor =
        when {
            change.delta > 0 -> RuleUpTheme.colors.success
            change.delta < 0 -> RuleUpTheme.colors.danger
            else -> RuleUpTheme.colors.textMuted
        }
    val deltaLabel =
        when {
            change.delta > 0 -> "+${change.delta.trimLabel()}℃"
            change.delta < 0 -> "−${(-change.delta).trimLabel()}℃"
            else -> "0℃"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = change.label,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateDotLabel(change.date),
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(
            text = deltaLabel,
            color = deltaColor,
            style = RuleUpTheme.typography.cardTitle,
        )
    }
}
