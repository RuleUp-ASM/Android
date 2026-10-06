package com.ruleup.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.designsystem.R
import com.ruleup.designsystem.category.CategoryCover
import com.ruleup.designsystem.category.CategoryIconTile
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.time.ServiceDate
import com.ruleup.home.presentation.viewmodel.DayResult
import com.ruleup.home.presentation.viewmodel.HomeIntent
import com.ruleup.home.presentation.viewmodel.HomeState
import com.ruleup.home.presentation.viewmodel.HomeViewModel
import com.ruleup.home.presentation.viewmodel.SelectedDay
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.rememberTtiLargeContent
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.verification.domain.entity.TodayStatus
import java.time.LocalDate
import java.time.LocalTime

/** 홈 · 진행 중. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    // 백그라운드에서 돌아와도 다시 불러온다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(HomeIntent.Load) }
    HomeContent(modifier = modifier, state = state, onIntent = viewModel::onIntent)
}

/** 홈(Figma 1557:2 시안 ⑤). 인사 · 이번 주 · 오늘 해 볼 것 하나 · 매일 루틴 · 주 N회 루틴. */
@Composable
// 테스트에서 상태를 직접 넣어 렌더하려고 연다.
internal fun HomeContent(
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
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Brush.verticalGradient(listOf(RuleUpTheme.colors.brandSoft, RuleUpTheme.colors.background))),
        )
        val header: @Composable () -> Unit = {
            HomeHeader(
                nickname = state.nickname,
                subtitle = if (state.isEmpty) "첫 챌린지를 시작해 보세요" else homeSummary(state.challenges).label(),
                hasUnread = state.hasUnreadNotifications,
                onOpenNotifications = { onIntent(HomeIntent.OpenNotifications) },
            )
        }
        // 이번 주 · 히어로를 남기면 "0/0" 껍데기만 보여 처음 들어온 사람이 뭘 할지 모른다.
        if (state.isEmpty && state.starters.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().ttiContentDrawn(),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { header() }
                item { StarterSection(state = state, onIntent = onIntent) }
                if (state.interests.isNotEmpty()) {
                    item { InterestChips(interests = state.interests, onOpen = { onIntent(HomeIntent.OpenCategory(it)) }) }
                }
                item { StarterExploreButton(onClick = { onIntent(HomeIntent.OpenExplore) }) }
            }
        } else if (state.isEmpty) {
            // 첫 챌린지 후보를 못 받았을 때의 기본 안내.
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                header()
                HomeEmptyState(
                    modifier = Modifier.weight(1f).ttiContentDrawn(),
                    onExplore = { onIntent(HomeIntent.OpenExplore) },
                    onCreate = { onIntent(HomeIntent.CreateChallenge) },
                )
            }
        } else {
            val hero = state.hero
            val rest = state.challenges.filter { it.challengeId != hero?.challengeId }
            val daily = rest.filter { it.isDaily }
            val weekly = rest.filterNot { it.isDaily }
            val open: (HomeChallengeUi) -> Unit = { onIntent(HomeIntent.OpenChallenge(it.challengeId)) }
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().ttiContentDrawn(),
                // 떠 있는 탭 바에 마지막 줄이 가리지 않게 바 높이만큼 비운다.
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { header() }
                item {
                    WeekStamps(
                        statuses = state.weekStatuses,
                        selectedDate = state.selectedDay?.date,
                        onSelect = { onIntent(HomeIntent.SelectDay(it)) },
                    )
                }
                val selected = state.selectedDay
                if (selected != null) {
                    item { DayResultSection(selected = selected, onIntent = onIntent) }
                    return@LazyColumn
                }
                if (hero != null) {
                    item { SectionTitle(title = "오늘 해 볼까요?", trailing = "직접 체크") }
                    item { HeroCard(card = hero, onOpen = { open(hero) }) }
                }
                if (daily.isNotEmpty()) {
                    item { SectionTitle(title = "오늘 · 매일 루틴") }
                    item { RoutineList(cards = daily, onOpen = open) }
                }
                if (weekly.isNotEmpty()) {
                    item { SectionTitle(title = "이번 주 · 주 N회") }
                    item { RoutineList(cards = weekly, onOpen = open) }
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
                    RuleUpBottomTab.CHALLENGE -> onIntent(HomeIntent.OpenMyChallenges)
                    RuleUpBottomTab.HOME -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** 홈 빈 상태. */
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

/** 홈 상단: 인사 + 오늘 요약 + 알림. */
@Composable
private fun HomeHeader(
    nickname: String?,
    subtitle: String,
    hasUnread: Boolean,
    onOpenNotifications: () -> Unit,
) {
    val hour = remember { LocalTime.now(ServiceDate.ZONE).hour }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = greeting(hour) + (nickname?.let { ", ${it}님" } ?: ""),
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.small,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.surface)
                    .singleClickable(onClick = onOpenNotifications),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = if (hasUnread) "알림 (읽지 않은 알림 있음)" else "알림",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
            if (hasUnread) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(RuleUpTheme.colors.danger),
                )
            }
        }
    }
}

/** 신규 이용자: 바로 들어갈 수 있는 첫 챌린지 카드(Figma 1563:2). */
@Composable
private fun StarterSection(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val matched = state.starters.any { it.category != null && it.category in state.interests }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "이런 챌린지로 시작해 보세요", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
            Text(
                text =
                    if (matched) {
                        "관심 분야(${state.interests.joinToString(" · ") { it.label }})에서 지금 많이 모이는 방이에요"
                    } else {
                        "지금 많이 모이는 방이에요"
                    },
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.starters, key = { it.challengeId }) { item ->
                StarterCard(item = item, onClick = { onIntent(HomeIntent.OpenChallenge(item.challengeId)) })
            }
        }
    }
}

@Composable
private fun StarterCard(
    item: TrendingChallenge,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier =
            Modifier
                .size(width = 200.dp, height = 250.dp)
                .shadow(6.dp, shape, clip = false)
                .clip(shape)
                .singleClickable(onClick = onClick),
    ) {
        CategoryCover(category = item.category, modifier = Modifier.matchParentSize())
        item.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to HeroScrim)))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item.category?.let {
                Text(
                    text = it.label,
                    color = Color.White,
                    style = RuleUpTheme.typography.captionBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.24f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            Text(
                text = item.title,
                color = Color.White,
                style = RuleUpTheme.typography.cardTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.participantCount}명 참여 중 · " + if (item.verificationType.isAuto) "자동 인증" else "직접 체크",
                color = Color.White.copy(alpha = 0.88f),
                style = RuleUpTheme.typography.captionMedium,
            )
        }
    }
}

@Composable
private fun InterestChips(
    interests: List<Category>,
    onOpen: (Category) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "관심 분야에서 찾아보기", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            interests.forEach { category ->
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(99.dp))
                            .singleClickable { onOpen(category) }
                            .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIconTile(category = category, size = 24.dp)
                    Text(text = category.label, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.smallBold)
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RuleUpTheme.colors.textMuted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StarterExploreButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EmptyActionButton(
            text = "챌린지 더 둘러보기",
            background = RuleUpTheme.colors.brand,
            textColor = Color.White,
            onClick = onClick,
        )
        // 만들기는 탭 바 + 와 겹치므로 버튼 대신 안내만 둔다.
        Text(text = "직접 만들려면 오른쪽 아래 + 를 눌러요", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
    }
}

/** 「오늘 할 일 1/3개 · 이번 주 목표 2개」. */
private fun HomeSummary.label(): String {
    val parts =
        listOfNotNull(
            "오늘 할 일 $dailyDone/${dailyTotal}개".takeIf { dailyTotal > 0 },
            "이번 주 목표 ${weeklyTotal}개".takeIf { weeklyTotal > 0 },
        )
    return parts.joinToString(" · ").ifEmpty { "오늘 정해진 할 일은 없어요" }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
            modifier = Modifier.weight(1f),
        )
        trailing?.let { Text(text = it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption) }
    }
}

/** 이번 주 도장. ✓ 그날 다 지킴 · ✕ 하나라도 못 지킴 · 테두리 오늘. 지난 날을 누르면 그날 결과를 본다. */
@Composable
private fun WeekStamps(
    statuses: Map<String, CalendarDayStatus>,
    selectedDate: String?,
    onSelect: (String) -> Unit,
) {
    val today =
        com.ruleup.ui.time
            .rememberServiceDate()
    val monday = remember(today) { today.minusDays((today.dayOfWeek.value - 1).toLong()) }
    val labels = listOf("월", "화", "수", "목", "금", "토", "일")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (i in 0..6) {
            val date = monday.plusDays(i.toLong())
            val stamp = dayStamp(date, today, statuses[date.toString()])
            // 아무것도 고르지 않았으면 오늘이 선택된 칸이다.
            val selected = (selectedDate ?: today.toString()) == date.toString()
            Column(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .then(if (stamp == DayStamp.FUTURE) Modifier else Modifier.singleClickable { onSelect(date.toString()) })
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = labels[i],
                    color = if (selected) RuleUpTheme.colors.brand else RuleUpTheme.colors.textMuted,
                    style = if (selected) RuleUpTheme.typography.captionBold else RuleUpTheme.typography.caption,
                )
                DayStampMark(stamp = stamp, dayOfMonth = date.dayOfMonth, selected = selected)
            }
        }
    }
}

@Composable
private fun DayStampMark(
    stamp: DayStamp,
    dayOfMonth: Int,
    selected: Boolean,
) {
    val colors = RuleUpTheme.colors
    // 고른 칸은 바깥 고리로 띄운다.
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .border(2.dp, if (selected) colors.brand else Color.Transparent, CircleShape)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (stamp) {
            DayStamp.DONE, DayStamp.FAILED ->
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (stamp == DayStamp.DONE) colors.success else colors.danger),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (stamp == DayStamp.DONE) R.drawable.ic_check else R.drawable.ic_close),
                        contentDescription = if (stamp == DayStamp.DONE) "${dayOfMonth}일 다 지켰어요" else "${dayOfMonth}일 못 지킨 루틴이 있어요",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            DayStamp.TODAY, DayStamp.EMPTY, DayStamp.FUTURE ->
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (stamp == DayStamp.TODAY) colors.surface else colors.surfaceVariant)
                            .border(if (stamp == DayStamp.TODAY) 2.dp else 0.dp, colors.brand, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dayOfMonth.toString(),
                        color =
                            when (stamp) {
                                DayStamp.TODAY -> colors.brand
                                DayStamp.FUTURE -> colors.textMuted
                                else -> colors.textSecondary
                            },
                        style = RuleUpTheme.typography.smallBold,
                    )
                }
        }
    }
}

/** 고른 날의 결과. 홈의 히어로 · 매일 루틴 · 주 N회 자리를 대신한다. */
@Composable
private fun DayResultSection(
    selected: SelectedDay,
    onIntent: (HomeIntent) -> Unit,
) {
    val date = remember(selected.date) { LocalDate.parse(selected.date) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.headerLabel(),
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "오늘로",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.smallBold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .singleClickable { onIntent(HomeIntent.SelectDay(selected.date)) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        when (val result = selected.result) {
            DayResult.Loading ->
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }
            DayResult.Failed ->
                DayMessage(text = "이날 기록을 불러오지 못했어요", action = "다시 시도", onAction = { onIntent(HomeIntent.RetryDay) })
            is DayResult.Loaded -> {
                val groups = result.items.groupByResult()
                if (result.items.isEmpty()) {
                    DayMessage(text = "이날은 판정할 루틴이 없었어요")
                } else {
                    val open: (String) -> Unit = { onIntent(HomeIntent.OpenChallenge(it)) }
                    DayGroup(title = "수행한 루틴", items = groups.done, done = true, onOpen = open)
                    DayGroup(title = "실패한 루틴", items = groups.failed, done = false, onOpen = open)
                    DayGroup(title = "판정 중", items = groups.pending, done = null, onOpen = open)
                }
            }
        }
    }
}

@Composable
private fun DayMessage(
    text: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = text, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
        action?.let {
            Text(
                text = it,
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.bodyBold,
                modifier = Modifier.singleClickable(onClick = onAction),
            )
        }
    }
}

/** done: true 수행 · false 실패 · null 판정 중. 비어 있으면 그리지 않는다. */
@Composable
private fun DayGroup(
    title: String,
    items: List<CalendarDayItem>,
    done: Boolean?,
    onOpen: (String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title = "$title ${items.size}")
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, shape)
                    .padding(vertical = 4.dp),
        ) {
            items.forEach { item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .singleClickable { onOpen(item.challengeId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIconTile(category = item.category, size = 36.dp)
                    Text(
                        text = item.title,
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.bodyBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (done != null) {
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (done) RuleUpTheme.colors.success else RuleUpTheme.colors.danger),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(if (done) R.drawable.ic_check else R.drawable.ic_close),
                                contentDescription = if (done) "수행" else "실패",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 「10월 5일 월요일」. */
private fun LocalDate.headerLabel(): String {
    val weekday = listOf("월", "화", "수", "목", "금", "토", "일")[dayOfWeek.ordinal]
    return "${monthValue}월 ${dayOfMonth}일 ${weekday}요일"
}

/** 오늘 직접 체크해 볼 만한 챌린지 하나. 대표 사진이 없으면 카테고리 기본 커버. */
@Composable
private fun HeroCard(
    card: HomeChallengeUi,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier =
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(200.dp)
                .shadow(8.dp, shape, clip = false)
                .clip(shape)
                .singleClickable(onClick = onOpen),
    ) {
        CategoryCover(category = card.category, modifier = Modifier.matchParentSize())
        card.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val onImageSettled = rememberTtiLargeContent()
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { onImageSettled() },
                onError = { onImageSettled() },
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier =
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0.3f to Color.Transparent, 1f to HeroScrim),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = card.cadenceLabel() + " · 직접 체크",
                color = Color.White,
                style = RuleUpTheme.typography.captionBold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
            )
            Text(
                text = card.title,
                color = Color.White,
                style = RuleUpTheme.typography.numberM,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (card.isDaily) "오늘 안에 체크해요" else "이번 주 안에 하면 돼요",
                    color = Color.White.copy(alpha = 0.88f),
                    style = RuleUpTheme.typography.smallMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "체크하기",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.smallBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White)
                            .singleClickable(onClick = onOpen)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private val HeroScrim = Color(0xFF0D1440).copy(alpha = 0.78f)

/** 「매일」 · 「주 3회」. */
private fun HomeChallengeUi.cadenceLabel(): String = if (isDaily) "매일" else "주 ${weeklyCount}회"

@Composable
private fun RoutineList(
    cards: List<HomeChallengeUi>,
    onOpen: (HomeChallengeUi) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier =
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, shape)
                .padding(vertical = 4.dp),
    ) {
        cards.forEach { card -> RoutineRow(card = card, onClick = { onOpen(card) }) }
    }
}

@Composable
private fun RoutineRow(
    card: HomeChallengeUi,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .singleClickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconTile(category = card.category, size = 36.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = card.title,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.cadenceLabel() + " · " + card.subtitle,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        RoutineTrailing(card)
    }
}

@Composable
private fun RoutineTrailing(card: HomeChallengeUi) {
    when {
        // 시작 전 · 강퇴는 부제가 이미 말한다.
        !card.active -> Unit
        card.todayStatus == TodayStatus.DONE ->
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(RuleUpTheme.colors.success),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "오늘 완료",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        card.isDaily -> {
            val (label, color) =
                when (card.todayStatus) {
                    TodayStatus.FAILED -> "실패" to RuleUpTheme.colors.danger
                    TodayStatus.FAIL_EXPECTED -> "실패 위험" to RuleUpTheme.colors.warning
                    TodayStatus.NOT_TARGET -> "쉬는 날" to RuleUpTheme.colors.textMuted
                    TodayStatus.IN_PROGRESS, TodayStatus.DONE, null -> "진행 중" to RuleUpTheme.colors.brand
                }
            Text(text = label, color = color, style = RuleUpTheme.typography.smallBold)
        }
        // TODO(#584): 이번 주 횟수(n/N회)를 서버가 주면 기간 진행률 자리를 바꾼다.
        else ->
            Text(
                text = "진행 ${(card.progress * 100).toInt()}%",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallBold,
                textAlign = TextAlign.End,
            )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HomeContentPreview() {
    RuleUpTheme {
        HomeContent(
            state = HomeState.initial.copy(isLoading = false),
            onIntent = { },
        )
    }
}
