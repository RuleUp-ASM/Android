package com.ruleup.home.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.R
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.home.presentation.viewmodel.HomeFilter
import com.ruleup.home.presentation.viewmodel.HomeIntent
import com.ruleup.home.presentation.viewmodel.HomeState
import com.ruleup.home.presentation.viewmodel.HomeViewModel
import java.time.LocalDate

// 아바타·오늘 카드 그라데이션. Figma 디자인 시스템에 그라데이션 토큰이 없어 팔레트 두 색으로 만든다.
private val AvatarGradient = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300)
private val TodayGradient = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300)

/** 홈 · 진행 중. 온보딩/로그인 완료 후 진입하는 루트 화면(Figma 01 · 홈 진행중). */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(HomeIntent.Load) }
    HomeContent(modifier = modifier, state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            HomeHeader()

            // 스트릭 카드·필터 탭을 남기면 "0/0" 껍데기만 보여 처음 들어온 사람이 뭘 할지 모른다.
            // 그래서 화면 전체를 빈 상태로 바꾼다(Figma 1134:2033).
            if (state.isEmpty) {
                HomeEmptyState(
                    modifier = Modifier.weight(1f),
                    onExplore = { onIntent(HomeIntent.OpenExplore) },
                    onCreate = { onIntent(HomeIntent.CreateChallenge) },
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { WeekStreakCard() }
                    item {
                        FilterTabs(
                            filter = state.filter,
                            activeCount = state.activeCount,
                            todayCount = state.todayCount,
                            onSelect = { onIntent(HomeIntent.SelectFilter(it)) },
                        )
                    }
                    items(state.visibleChallenges, key = { it.challengeId }) { card ->
                        ChallengeCard(card = card, onClick = { onIntent(HomeIntent.OpenChallenge(card.challengeId)) })
                    }
                }
            }
        }

        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.HOME,
            selectedColor = RuleUpTheme.colors.brand,
            onCreateClick = { onIntent(HomeIntent.CreateChallenge) },
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.EXPLORE -> onIntent(HomeIntent.OpenExplore)
                    RuleUpBottomTab.MY -> onIntent(HomeIntent.OpenMy)
                    // TODO(#269): "내 챌린지" 목적지 미정 — 화면도 라우트 등록도 아직 없다.
                    RuleUpBottomTab.CHALLENGE -> Unit
                    RuleUpBottomTab.HOME -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 홈 빈 상태 (Figma 1134:2051).
 * 다음 행동을 둘로만 좁힌다 — 남의 방에 들어가거나, 내가 만들거나. 둘 중 뭘 해도 홈이 채워진다.
 */
@Composable
private fun HomeEmptyState(
    onExplore: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = RuleUpTheme.colors.brand,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "첫 습관을 시작해 볼까요?",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "함께할 방을 찾거나\n직접 만들 수 있어요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        EmptyActionButton(
            text = "챌린지 둘러보기",
            background = RuleUpTheme.colors.brand,
            textColor = Color.White,
            onClick = onExplore,
        )
        Spacer(Modifier.height(8.dp))
        EmptyActionButton(
            text = "직접 만들기",
            background = RuleUpTheme.colors.surface,
            textColor = RuleUpTheme.colors.textPrimary,
            bordered = true,
            onClick = onCreate,
        )
    }
}

@Composable
private fun EmptyActionButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .let { base ->
                    if (bordered) base.border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp)) else base
                }.singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = textColor, style = RuleUpTheme.typography.cardTitle)
    }
}

/** 홈 상단 (Figma 1134:2045). 오늘 날짜 + 알림. 인사말 대신 날짜를 두면 "오늘 뭘 했나"로 시선이 간다. */
@Composable
private fun HomeHeader() {
    val today = remember { LocalDate.now() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = today.headerLabel(),
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.numberM,
        )
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = "알림",
                tint = RuleUpTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** "8월 3일 월요일". 연도는 빼고 오늘만 읽히게 둔다. */
private fun LocalDate.headerLabel(): String {
    val weekday = listOf("월", "화", "수", "목", "금", "토", "일")[dayOfWeek.ordinal]
    return "${monthValue}월 ${dayOfMonth}일 ${weekday}요일"
}

@Composable
private fun WeekStreakCard() {
    val today = remember { LocalDate.now() }
    val monday = remember(today) { today.minusDays((today.dayOfWeek.value - 1).toLong()) }
    val labels = listOf("월", "화", "수", "목", "금", "토", "일")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "이번 주",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (i in 0..6) {
                val date = monday.plusDays(i.toLong())
                val isToday = date == today
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isToday) {
                                    Modifier.background(Brush.verticalGradient(TodayGradient))
                                } else {
                                    Modifier
                                },
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = labels[i],
                        color = if (isToday) Color.White else RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.micro,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = if (isToday) Color.White else RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.bodyBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isToday) Color.White.copy(alpha = 0.35f) else RuleUpTheme.colors.surfaceVariant,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    filter: HomeFilter,
    activeCount: Int,
    todayCount: Int,
    onSelect: (HomeFilter) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterTab(
            text = "진행 중 $activeCount",
            selected = filter == HomeFilter.ACTIVE,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(HomeFilter.ACTIVE) },
        )
        FilterTab(
            text = "오늘 할 일 $todayCount",
            selected = filter == HomeFilter.TODAY,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(HomeFilter.TODAY) },
        )
    }
}

@Composable
private fun FilterTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .then(
                    if (selected) {
                        Modifier
                            .shadow(2.dp, RoundedCornerShape(10.dp), clip = false)
                            .background(RuleUpTheme.colors.surface)
                    } else {
                        Modifier
                    },
                ).singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textSecondary,
            style = if (selected) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ChallengeCard(
    card: HomeChallengeUi,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .singleClickable(onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(card.accentColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(card.iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = card.title,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.cardTitle,
                )
                Text(
                    text = card.subtitle,
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(RuleUpTheme.colors.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(card.progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(card.accentColor),
            )
        }
    }
}

@Composable
private fun EmptyChallenges() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "아직 진행 중인 챌린지가 없어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Text(
            text = "오른쪽 아래 + 버튼으로 첫 챌린지를 만들어 보세요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
    }
}
