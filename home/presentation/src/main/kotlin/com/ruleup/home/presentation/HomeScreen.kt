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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.R
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.home.presentation.viewmodel.HomeFilter
import com.ruleup.home.presentation.viewmodel.HomeIntent
import com.ruleup.home.presentation.viewmodel.HomeState
import com.ruleup.home.presentation.viewmodel.HomeViewModel
import java.time.LocalDate

private val FigmaBrand = Color(0xFF6C5CE7)
private val AvatarGradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
private val TodayGradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))

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
                val cards = state.visibleChallenges
                if (cards.isEmpty() && !state.isLoading) {
                    item { EmptyChallenges() }
                } else {
                    items(cards, key = { it.challengeId }) { card ->
                        ChallengeCard(card = card, onClick = { onIntent(HomeIntent.OpenChallenge(card.challengeId)) })
                    }
                }
            }
        }

        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.HOME,
            selectedColor = FigmaBrand,
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.CHALLENGE -> onIntent(HomeIntent.OpenExplore)
                    RuleUpBottomTab.MY -> onIntent(HomeIntent.OpenMy)
                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // FAB · 챌린지 추가. 하단 탭 위에 떠 있도록 오프셋.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 84.dp)
                    .size(56.dp)
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(FigmaBrand)
                    .singleClickable { onIntent(HomeIntent.CreateChallenge) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = "챌린지 추가",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "안녕하세요 👋",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "오늘도 함께 해볼까요?",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 11.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RuleUpTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = "알림",
                    tint = RuleUpTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(AvatarGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "R", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = if (isToday) Color.White else RuleUpTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
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
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = card.subtitle,
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 11.sp,
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "오른쪽 아래 + 버튼으로 첫 챌린지를 만들어 보세요",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 12.sp,
        )
    }
}
