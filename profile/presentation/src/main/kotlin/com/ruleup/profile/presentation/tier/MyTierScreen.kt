package com.ruleup.profile.presentation.tier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpProgressBar
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.presentation.common.accentColor
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.common.deltaLabel
import com.ruleup.profile.presentation.common.label
import com.ruleup.profile.presentation.common.scoreRangeLabel
import com.ruleup.profile.presentation.common.thousandsLabel
import com.ruleup.profile.presentation.common.titleLabel
import com.ruleup.profile.presentation.tier.viewmodel.MyTierIntent
import com.ruleup.profile.presentation.tier.viewmodel.MyTierState
import com.ruleup.profile.presentation.tier.viewmodel.MyTierViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn

/** 내 티어. */
@Composable
fun MyTierScreen(
    modifier: Modifier = Modifier,
    viewModel: MyTierViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyTierIntent.Load)
    }

    MyTierContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun MyTierContent(
    state: MyTierState,
    onIntent: (MyTierIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "내 티어", onBack = { onIntent(MyTierIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.tier == null ->
                Column(
                    Modifier.fillMaxSize().ttiContentDrawn(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "티어 정보를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { onIntent(MyTierIntent.Load) }) { Text("다시 시도") }
                }

            else ->
                TierBody(
                    tier = state.tier,
                    onOpenHistory = { onIntent(MyTierIntent.OpenHistory) },
                )
        }
    }
}

@Composable
private fun TierBody(
    tier: MyTier,
    onOpenHistory: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp)
                .ttiContentDrawn(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TierHero(tier = tier)
        TierBandTable(current = tier.displayTier)
        RecentChangesCard(changes = tier.recentChanges, onOpenHistory = onOpenHistory)
        Text(
            text = "올라가는 건 즉시, 내려가는 건 20점의 여유가 있어요 · 가입하면 브론즈 10점에서 시작해요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

/** 현재 티어·점수와 다음 티어까지의 진행. */
@Composable
private fun TierHero(tier: MyTier) {
    TierCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            TierChip(tier = tier.displayTier)
            Box(Modifier.weight(1f))
            // 유예 중에는 올라갈 자리가 아니라 지켜야 할 바닥을 말한다
            val graceFloor = tier.graceFloorScore
            if (graceFloor != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${tier.displayTier.label} 유지선",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.caption,
                    )
                    ScoreText(
                        value = graceFloor,
                        color = RuleUpTheme.colors.danger,
                        style = RuleUpTheme.typography.numberM,
                    )
                }
            } else {
                tier.displayPromotion?.let { promotion ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${promotion.nextTier.label}까지",
                            color = RuleUpTheme.colors.textMuted,
                            style = RuleUpTheme.typography.caption,
                        )
                        ScoreText(
                            value = promotion.pointsToPromote,
                            color = RuleUpTheme.colors.brand,
                            style = RuleUpTheme.typography.numberM,
                        )
                    }
                }
            }
        }
        ScoreText(
            value = tier.score,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.numberXl,
        )
        RuleUpProgressBar(progress = tier.progressInDisplayTier)
        if (tier.graceFloorScore != null) {
            Text(
                text = "점수가 내려갔지만 ${tier.displayTier.label}를 유지하고 있어요. 더 내려가면 강등돼요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${tier.displayTier.label} ${tier.displayTier.minScore.thousandsLabel()}",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
            Box(Modifier.weight(1f))
            // 오른쪽 끝은 표시 티어보다 위인 다음 구간이다.
            tier.displayPromotion?.let {
                Text(
                    text = "${it.nextTier.label} ${it.nextTier.minScore.thousandsLabel()}",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
    }
}

/** 숫자 + "점". */
@Composable
private fun ScoreText(
    value: Int,
    color: Color,
    style: TextStyle,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = value.thousandsLabel(), color = color, style = style)
        Text(
            text = "점",
            color = color,
            style = RuleUpTheme.typography.smallBold,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
        )
    }
}

/** 5구간 표. */
@Composable
private fun TierBandTable(current: Tier) {
    TierCard(contentPadding = 0.dp) {
        Tier.entries.reversed().forEachIndexed { index, tier ->
            if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
            val isCurrent = tier == current
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(if (isCurrent) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surface)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tier.label,
                    color = tier.accentColor,
                    style = RuleUpTheme.typography.bodyBold,
                )
                Text(
                    text = tier.scoreRangeLabel,
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.small,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Box(Modifier.weight(1f))
                if (isCurrent) {
                    Text(
                        text = "지금",
                        color = RuleUpTheme.colors.brand,
                        style = RuleUpTheme.typography.smallBold,
                    )
                }
            }
        }
    }
}

/** 최근 변동. */
@Composable
private fun RecentChangesCard(
    changes: List<ScoreChange>,
    onOpenHistory: () -> Unit,
) {
    TierCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "최근 변동",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
            )
            Box(Modifier.weight(1f))
            Text(
                text = "전체 보기",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.smallBold,
                modifier = Modifier.singleClickable(onClick = onOpenHistory),
            )
        }
        if (changes.isEmpty()) {
            Text(
                text = "아직 점수가 움직인 적이 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
            )
        }
        changes.forEach { change ->
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.width(64.dp),
                )
            }
        }
    }
}

/** 카드 한 장. */
@Composable
private fun TierCard(
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** 현재 표시 티어 칩. */
@Composable
private fun TierChip(tier: Tier) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = tier.label,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.smallBold,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MyTierContentPreview() {
    RuleUpTheme {
        MyTierContent(
            state =
                com.ruleup.profile.presentation.tier.viewmodel.MyTierState.initial
                    .copy(isLoading = false),
            onIntent = { },
        )
    }
}
