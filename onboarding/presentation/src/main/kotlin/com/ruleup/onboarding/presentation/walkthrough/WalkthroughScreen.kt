package com.ruleup.onboarding.presentation.walkthrough

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.presentation.R
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughIntent
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughPageIndex
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughState
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughViewModel

/**
 * 첫 실행 워크쓰루(Figma `1460:2`·`1461:2`·`1462:2`).
 *
 * 세 장의 뼈대가 같다 — 상단바 · 일러스트 무대 · 문구 · 점 표시 + CTA. 그래서 [WalkthroughFrame]
 * 하나에 장마다 다른 무대만 끼워 넣는다. 장을 옮길 때 문구와 점이 어긋나는 흔한 실수를 구조로 막는다.
 */
@Composable
fun WalkthroughScreen(viewModel: WalkthroughViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WalkthroughContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun WalkthroughContent(
    state: WalkthroughState,
    onIntent: (WalkthroughIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    WalkthroughFrame(
        page = state.page,
        onSkip = { onIntent(WalkthroughIntent.Skip) },
        onNext = { onIntent(WalkthroughIntent.Next) },
        modifier = modifier,
    ) {
        when (state.page) {
            WalkthroughPageIndex.CREATE -> CreateStage()
            WalkthroughPageIndex.KEEP -> KeepStage()
            WalkthroughPageIndex.STAY -> StayStage()
        }
    }
}

/** 세 장이 공유하는 뼈대. 문구는 장마다 다르지만 위치·간격은 하나로 묶는다. */
@Composable
private fun WalkthroughFrame(
    page: WalkthroughPageIndex,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    stage: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.surface),
    ) {
        WalkthroughTopBar(
            // 마지막 장에는 건너뛰기가 없다 — CTA 가 곧 끝내기라 같은 일을 하는 버튼이 둘이 된다.
            onSkip = onSkip.takeUnless { page.isLast },
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = RuleUpTheme.spacing.sm, start = 20.dp, end = 20.dp)
                    .clip(RuleUpTheme.shapes.sheet)
                    .background(RuleUpTheme.colors.brandSoft)
                    .padding(horizontal = 20.dp, vertical = RuleUpTheme.spacing.xxl),
            contentAlignment = Alignment.Center,
        ) {
            stage()
        }
        WalkthroughCopy(page = page)
        WalkthroughFooter(page = page, onNext = onNext)
    }
}

@Composable
private fun WalkthroughTopBar(onSkip: (() -> Unit)?) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("RuleUp", color = RuleUpTheme.colors.brand, style = RuleUpTheme.typography.section)
        if (onSkip != null) {
            Text(
                text = "건너뛰기",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.bodyMedium,
                modifier = Modifier.singleClickable(onClick = onSkip),
            )
        }
    }
}

@Composable
private fun WalkthroughCopy(page: WalkthroughPageIndex) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, start = RuleUpTheme.spacing.xxl, end = RuleUpTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        Text(page.eyebrow, color = RuleUpTheme.colors.brand, style = RuleUpTheme.typography.captionBold)
        Text(
            text = page.title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
        )
        Text(
            text = page.description,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
        )
    }
}

@Composable
private fun WalkthroughFooter(
    page: WalkthroughPageIndex,
    onNext: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = RuleUpTheme.spacing.xxl, bottom = 28.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PageDots(current = page)
        RuleUpPrimaryButton(text = if (page.isLast) "시작하기" else "다음", onClick = onNext)
    }
}

/** 현재 장만 길쭉한 알약이 된다. 개수는 [WalkthroughPageIndex] 가 정한다 — 장이 늘면 점도 따라 는다. */
@Composable
private fun PageDots(current: WalkthroughPageIndex) {
    Row(horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs)) {
        WalkthroughPageIndex.entries.forEach { page ->
            val active = page == current
            Box(
                modifier =
                    Modifier
                        .height(DOT_SIZE)
                        .width(if (active) ACTIVE_DOT_WIDTH else DOT_SIZE)
                        .clip(RuleUpTheme.shapes.pill)
                        .background(if (active) RuleUpTheme.colors.brand else RuleUpTheme.colors.border),
            )
        }
    }
}

// ---------- 01 · 만들기 ----------

@Composable
private fun CreateStage() {
    Column(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md)) {
        StageCard {
            Text("어떤 루틴을 만들까요?", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "매일 아침 6시 30분에 일어나기",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyMedium,
                )
                Box(
                    modifier =
                        Modifier
                            .padding(start = 2.dp)
                            .width(1.5.dp)
                            .height(16.dp)
                            .background(RuleUpTheme.colors.brand),
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .clip(RuleUpTheme.shapes.pill)
                    .background(RuleUpTheme.colors.brand)
                    .padding(horizontal = RuleUpTheme.spacing.md, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wt_sparkles),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text("AI가 초안을 만들었어요", color = Color.White, style = RuleUpTheme.typography.captionBold)
        }
        StageCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(
                    icon = R.drawable.ic_wt_sun,
                    tint = RuleUpTheme.colors.warning,
                    background = RuleUpTheme.colors.warningContainer,
                    size = 36.dp,
                    iconSize = 18.dp,
                    shape = RuleUpTheme.shapes.small,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("아침 6:30 기상", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
                    Text("기상 · 그룹 챌린지", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
                }
                Box(
                    modifier =
                        Modifier
                            .clip(RuleUpTheme.shapes.pill)
                            .background(RuleUpTheme.colors.brandSoft)
                            .padding(horizontal = RuleUpTheme.spacing.sm, vertical = 3.dp),
                ) {
                    Text("AI 초안", color = RuleUpTheme.colors.brand, style = RuleUpTheme.typography.tinyBold)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RuleUpTheme.colors.border))
            DraftRow(icon = R.drawable.ic_wt_shield_check, label = "인증 방법", value = "기상 신호 · 자동")
            DraftRow(icon = R.drawable.ic_wt_target, label = "목표", value = "매일 06:30 이전")
            DraftRow(icon = R.drawable.ic_wt_calendar, label = "기간", value = "2주")
        }
    }
}

@Composable
private fun DraftRow(
    icon: Int,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = RuleUpTheme.colors.textMuted,
            modifier = Modifier.size(14.dp),
        )
        Text(label, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.small)
        Text(
            text = value,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.smallBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

// ---------- 02 · 지키기 ----------

@Composable
private fun KeepStage() {
    Column(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SignalCard(R.drawable.ic_wt_map_pin, "위치", "장소 도착 확인", Modifier.weight(1f))
            SignalCard(R.drawable.ic_wt_footprints, "걸음", "하루 걸음 수", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SignalCard(R.drawable.ic_wt_smartphone, "앱 사용 시간", "스크린타임", Modifier.weight(1f))
            SignalCard(R.drawable.ic_wt_alarm_clock, "기상", "기상 시각", Modifier.weight(1f))
        }
        StageCard(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(
                    icon = R.drawable.ic_wt_check,
                    tint = RuleUpTheme.colors.success,
                    background = RuleUpTheme.colors.successContainer,
                    size = 36.dp,
                    iconSize = 18.dp,
                    shape = RuleUpTheme.shapes.pill,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("오늘 인증 완료", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
                    Text(
                        "06:24 기상 확인 · 자동 판정",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.caption,
                    )
                }
                StatusChip(text = "성공", tone = StatusChipTone.Success)
            }
        }
        Text(
            "매일 00시 판정 · 03시 확정",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.tinyMedium,
        )
    }
}

@Composable
private fun SignalCard(
    icon: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.ruleUpCardSurface(PaddingValues(14.dp)),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        IconBadge(
            icon = icon,
            tint = RuleUpTheme.colors.brand,
            background = RuleUpTheme.colors.brandSoft,
            size = 32.dp,
            iconSize = 16.dp,
            shape = RuleUpTheme.shapes.card,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.smallBold)
            Text(description, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.tiny)
        }
    }
}

// ---------- 03 · 남기기 ----------

@Composable
private fun StayStage() {
    Column(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md)) {
        StageCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(
                    icon = R.drawable.ic_wt_trophy,
                    tint = TierPalette.GoldIcon,
                    background = TierPalette.GoldSoft,
                    size = 44.dp,
                    iconSize = 22.dp,
                    shape = RuleUpTheme.shapes.pill,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("골드 · 70점", color = TierPalette.GoldIcon, style = RuleUpTheme.typography.section)
                    Text(
                        "다이아까지 29점 남았어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.caption,
                    )
                }
                StatusChip(text = "+5점", tone = StatusChipTone.Success)
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RuleUpTheme.shapes.pill)
                        .background(RuleUpTheme.colors.border),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(TIER_PROGRESS)
                            .height(8.dp)
                            .clip(RuleUpTheme.shapes.pill)
                            .background(TierPalette.GoldFill),
                )
            }
        }
        Row(
            modifier = Modifier.ruleUpCardSurface(PaddingValues(14.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TierStep(R.drawable.ic_wt_shield, "브론즈", TierPalette.BronzeSoft, TierPalette.BronzeIcon)
            TierStep(R.drawable.ic_wt_medal, "실버", TierPalette.SilverSoft, TierPalette.SilverIcon)
            TierStep(R.drawable.ic_wt_trophy, "골드", TierPalette.GoldSoft, TierPalette.GoldIcon, current = true)
            TierStep(R.drawable.ic_wt_gem, "다이아", TierPalette.DiamondSoft, TierPalette.DiamondIcon)
            TierStep(R.drawable.ic_wt_crown, "루비", TierPalette.RubySoft, TierPalette.RubyIcon)
        }
        Row(
            modifier = Modifier.ruleUpCardSurface(PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row {
                Avatar("지", RuleUpTheme.colors.brand)
                Avatar("서", RuleUpTheme.colors.brandAccent, Modifier.padding(start = 20.dp))
                Avatar("민", RuleUpTheme.colors.textMuted, Modifier.padding(start = 20.dp))
            }
            Text(
                "지현 님 외 4명과 함께 기록 중",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.smallMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 티어 사다리의 한 칸. 현재 티어만 테두리와 진한 라벨로 짚어 준다. */
@Composable
private fun TierStep(
    icon: Int,
    label: String,
    background: Color,
    tint: Color,
    current: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RuleUpTheme.shapes.pill)
                    .background(background)
                    .then(
                        if (current) {
                            Modifier.border(2.dp, TierPalette.GoldFill, RuleUpTheme.shapes.pill)
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            color = if (current) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
            style = if (current) RuleUpTheme.typography.tinyBold else RuleUpTheme.typography.tinyMedium,
        )
    }
}

@Composable
private fun Avatar(
    initial: String,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(RuleUpTheme.shapes.pill)
                .background(background)
                .border(2.dp, RuleUpTheme.colors.surface, RuleUpTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Color.White, style = RuleUpTheme.typography.tinyBold)
    }
}

// ---------- 공통 조각 ----------

/** 무대 위의 흰 카드. 세 장이 같은 표면을 쓰므로 여백만 바꿔 끼운다. */
@Composable
private fun StageCard(
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.ruleUpCardSurface(PaddingValues(horizontal = 16.dp, vertical = 14.dp)),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
private fun IconBadge(
    icon: Int,
    tint: Color,
    background: Color,
    size: Dp,
    iconSize: Dp,
    shape: RoundedCornerShape,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(shape)
                .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 골드 구간 진행률(70/100). 소개용 고정 값이라 상태로 올리지 않는다. */
private const val TIER_PROGRESS = 0.7f
private val DOT_SIZE = 6.dp
private val ACTIVE_DOT_WIDTH = 20.dp

/**
 * 티어 색. Figma 의 `primitive` 아래 티어별 변수인데 디자인 시스템 토큰 15종에는 없다 —
 * 지금 쓰는 화면이 여기뿐이라 팔레트를 늘리지 않고 이 파일에 둔다. 티어 화면이 색을 쓰기 시작하면
 * 그때 `RuleUpPalette` 로 올린다.
 */
private object TierPalette {
    val BronzeSoft = Color(0xFFF5EADF)
    val BronzeIcon = Color(0xFF8A5424)
    val SilverSoft = Color(0xFFEEF1F5)
    val SilverIcon = Color(0xFF5F6B7A)
    val GoldSoft = Color(0xFFFCF3D7)
    val GoldFill = Color(0xFFEAB308)
    val GoldIcon = Color(0xFFA16207)
    val DiamondSoft = Color(0xFFE0F4FE)
    val DiamondIcon = Color(0xFF0284C7)
    val RubySoft = Color(0xFFFDE7EE)
    val RubyIcon = Color(0xFFA8144A)
}

@Preview
@Composable
private fun WalkthroughCreatePreview() {
    RuleUpTheme { WalkthroughContent(state = WalkthroughState(WalkthroughPageIndex.CREATE), onIntent = {}) }
}

@Preview
@Composable
private fun WalkthroughKeepPreview() {
    RuleUpTheme { WalkthroughContent(state = WalkthroughState(WalkthroughPageIndex.KEEP), onIntent = {}) }
}

@Preview
@Composable
private fun WalkthroughStayPreview() {
    RuleUpTheme { WalkthroughContent(state = WalkthroughState(WalkthroughPageIndex.STAY), onIntent = {}) }
}
