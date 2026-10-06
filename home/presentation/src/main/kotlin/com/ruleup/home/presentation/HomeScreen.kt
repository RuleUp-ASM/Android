package com.ruleup.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ruleup.designsystem.R
import com.ruleup.designsystem.category.CategoryCover
import com.ruleup.designsystem.category.CategoryIconTile
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.time.ServiceDate
import com.ruleup.home.presentation.viewmodel.HomeIntent
import com.ruleup.home.presentation.viewmodel.HomeState
import com.ruleup.home.presentation.viewmodel.HomeViewModel
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
                summary = homeSummary(state.challenges),
                hasUnread = state.hasUnreadNotifications,
                onOpenNotifications = { onIntent(HomeIntent.OpenNotifications) },
            )
        }
        // 이번 주 · 히어로를 남기면 "0/0" 껍데기만 보여 처음 들어온 사람이 뭘 할지 모른다.
        if (state.isEmpty) {
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
                item { WeekPills(statuses = state.weekStatuses) }
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
    summary: HomeSummary,
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
                text = summary.label(),
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

/** 이번 주 요일 알약. 점은 그날 판정. */
@Composable
private fun WeekPills(statuses: Map<String, CalendarDayStatus>) {
    val today =
        com.ruleup.ui.time
            .rememberServiceDate()
    val monday = remember(today) { today.minusDays((today.dayOfWeek.value - 1).toLong()) }
    val labels = listOf("월", "화", "수", "목", "금", "토", "일")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 0..6) {
            val date = monday.plusDays(i.toLong())
            val isToday = date == today
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isToday) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.surface.copy(alpha = 0.75f))
                        .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = labels[i],
                    color = if (isToday) Color.White.copy(alpha = 0.7f) else RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.micro,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    color = if (isToday) Color.White else RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyBold,
                )
                Box(
                    modifier =
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(dayDotColor(date, today, statuses[date.toString()])),
                )
            }
        }
    }
}

/** 지난 날만 점을 찍는다 — 전부 성공은 초록, 나머지는 빨강. 오늘·아직 안 온 날·판정 대상이 아닌 날은 비운다. */
@Composable
private fun dayDotColor(
    date: LocalDate,
    today: LocalDate,
    status: CalendarDayStatus?,
): Color =
    when {
        !date.isBefore(today) || status == null -> Color.Transparent
        status == CalendarDayStatus.ALL_DONE -> RuleUpTheme.colors.success
        else -> RuleUpTheme.colors.danger
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
