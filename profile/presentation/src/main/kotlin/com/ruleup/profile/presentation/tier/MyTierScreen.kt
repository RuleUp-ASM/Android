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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.ruleup.profile.domain.entity.ScoreChangeReason
import com.ruleup.profile.domain.entity.TierDemotion
import com.ruleup.profile.presentation.common.accentColor
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.common.label
import com.ruleup.profile.presentation.common.scoreRangeLabel
import com.ruleup.profile.presentation.common.thousandsLabel
import com.ruleup.profile.presentation.tier.viewmodel.MyTierIntent
import com.ruleup.profile.presentation.tier.viewmodel.MyTierState
import com.ruleup.profile.presentation.tier.viewmodel.MyTierViewModel

/**
 * 내 티어 (Figma 1134:1520). 점수·다음 티어까지의 거리·구간표·최근 변동 10건.
 *
 * Figma 의 「이번 주 변동 +12 / 최대 ±20」 행은 그리지 않는다 — 명세가 `weeklyDelta` 를 폐기했다
 * (점수 한도가 챌린지별 사이클 순변동으로 바뀌면서 '계정 주간'이라는 단위가 사라졌다, 정책 §4.7).
 */
@Composable
fun MyTierScreen(
    modifier: Modifier = Modifier,
    viewModel: MyTierViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyTierIntent.Load)
    }

    MyTierContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "티어 정보를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
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
                .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TierHero(tier = tier)
        tier.demotion?.let { DemotionCard(displayTier = tier.displayTier, demotion = it) }
        TierBandTable(current = tier.displayTier)
        RecentChangesCard(changes = tier.recentChanges, onOpenHistory = onOpenHistory)
        Text(
            text = "올라가는 건 즉시, 내려가는 건 20점의 여유가 있어요 · 가입하면 브론즈 10점에서 시작해요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

/** 현재 티어·점수와 다음 티어까지의 진행 (Figma 1134:1538). */
@Composable
private fun TierHero(tier: MyTier) {
    TierCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            TierChip(tier = tier.displayTier)
            Box(Modifier.weight(1f))
            tier.promotion?.let { promotion ->
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
        ScoreText(
            value = tier.score,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.numberXl,
        )
        RuleUpProgressBar(progress = tier.progressInDisplayTier)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${tier.displayTier.label} ${tier.displayTier.minScore.thousandsLabel()}",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
            Box(Modifier.weight(1f))
            tier.promotion?.let {
                Text(
                    text = "${it.nextTier.label} ${it.nextTier.minScore.thousandsLabel()}",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
    }
}

/** 숫자 + "점". 숫자만 크게 두고 단위는 작게 붙인다(Figma 1134:1543). */
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

/**
 * 강등 안내 (Figma 1134:1562). 유예 구간이 있다는 사실 자체가 안심 문구라 강등 대상이면 항상 띄운다.
 *
 * 경계는 서버가 준 [TierDemotion.demoteAt] 을 그대로 쓴다 — 표시 티어 시작점 −21 이지만
 * 클라가 다시 계산하면 정책이 바뀔 때 두 곳이 어긋난다.
 */
@Composable
private fun DemotionCard(
    displayTier: Tier,
    demotion: TierDemotion,
) {
    val lower = Tier.entries.getOrNull(displayTier.ordinal - 1)
    TierCard {
        Text(
            text = "${displayTier.label}를 지키려면",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Text(
            text =
                "${displayTier.minScore.thousandsLabel()}점 아래로 내려가도 바로 떨어지지 않아요. " +
                    "${demotion.demoteAt.thousandsLabel()}점 이하가 되면 " +
                    (lower?.let { "${it.label}로 내려가요." } ?: "강등돼요."),
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
    }
}

/** 5구간 표 (Figma 1134:1565). 높은 티어가 위로 오도록 뒤집어 그린다. */
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

/** 최근 변동 (Figma 1134:1582). 서버 고정 10건이라 페이징이 없다. */
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
                    text = change.reason.label,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.small,
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
    }
}

/**
 * 사유 라벨.
 *
 * 명세의 `recentChanges[]` 에는 챌린지 제목이 없고 `challengeId` 만 온다 — Figma(1134:1588)가
 * 루틴 이름을 보여 주므로 서버에 `challengeTitle` 추가를 요청해 둔 상태다. 그때까지는 사유만 쓴다.
 */
private val ScoreChangeReason?.label: String
    get() =
        when (this) {
            ScoreChangeReason.CYCLE_SUCCESS -> "사이클 성공"
            ScoreChangeReason.CYCLE_FAIL -> "사이클 실패"
            ScoreChangeReason.LEAVE -> "중도 탈퇴"
            ScoreChangeReason.KICK_FAIL -> "연속 실패로 강퇴"
            ScoreChangeReason.KICK_PERMISSION -> "권한 미허용으로 강퇴"
            ScoreChangeReason.CHEAT -> "부정행위 검출"
            ScoreChangeReason.APPEAL_RESTORE -> "이의 인용으로 복원"
            // 모르는 사유는 증감폭만 남긴다 — 아무 라벨에나 접으면 그 행에 대해 거짓말이 된다.
            null -> "점수 변동"
        }

/** +8 / −5. 음수 기호는 하이픈이 아니라 U+2212 를 써야 숫자와 같은 높이로 붙는다. */
private val ScoreChange.deltaLabel: String
    get() = if (delta < 0) "−${-delta}" else "+$delta"

/** 카드 한 장. 티어 화면의 섹션은 전부 같은 흰 배경 + 라운드다(Figma 1134:1537). */
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

/** 현재 표시 티어 칩 (Figma 1134:1541). */
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
