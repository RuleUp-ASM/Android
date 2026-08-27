package com.ruleup.challenge.presentation.explore

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreIntent
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreState
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreViewModel
import com.ruleup.designsystem.R
import com.ruleup.designsystem.category.categoryIconRes
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

// 인기 1~3위 랭크 배지 그라데이션(Figma 탐색 메인).
private val TopRankGradient = listOf(Color(0xFFF97316), Color(0xFFEF4444))

/** 탐색 메인(Figma 01 · 탐색 메인). 실시간 인기 + 카테고리 그리드. 하단 탭 "챌린지" 로 진입한다. */
@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(ExploreIntent.Load) }

    ExploreContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun ExploreContent(
    state: ExploreState,
    onIntent: (ExploreIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 인기와 카테고리는 독립적으로 그린다 — 한쪽이 실패해도 다른 쪽은 그대로 보여야 한다.
            if (state.isTrendingLoading || !state.hideTrendingSection) {
                SectionHeader(title = "실시간 인기", onSeeAll = { onIntent(ExploreIntent.OpenTrendingAll) })
                Text(
                    text = "최근 24시간 참여 기준 · 1시간마다 갱신 · 그룹 챌린지만",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
                if (state.isTrendingLoading) {
                    TrendingSkeleton()
                } else {
                    TrendingCard(
                        trending = state.trending,
                        onClick = { onIntent(ExploreIntent.OpenChallenge(it)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            SectionHeader(title = "카테고리 탐색", onSeeAll = { onIntent(ExploreIntent.OpenCategoryAll) })
            when {
                state.isCategoriesLoading -> CategoryGridSkeleton()
                state.categoriesFailed -> SectionRetry(onRetry = { onIntent(ExploreIntent.RetryCategories) })
                else ->
                    CategoryGrid(
                        categories = state.categories,
                        onClick = { item ->
                            // 서버 표시명이 앱 카테고리와 매칭되면 필터 진입, 아니면 전체 목록으로.
                            val category = item.category
                            onIntent(
                                if (category != null) ExploreIntent.OpenCategory(category) else ExploreIntent.OpenCategoryAll,
                            )
                        },
                    )
            }
        }
        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.EXPLORE,
            onCreateClick = { onIntent(ExploreIntent.CreateChallenge) },
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.HOME -> onIntent(ExploreIntent.OpenHome)
                    RuleUpBottomTab.MY -> onIntent(ExploreIntent.OpenMy)
                    // TODO(#269): "내 챌린지" 목적지 미정.
                    RuleUpBottomTab.CHALLENGE -> Unit
                    RuleUpBottomTab.EXPLORE -> Unit
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "전체 ›",
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallMedium,
            modifier = Modifier.singleClickable(onClick = onSeeAll),
        )
    }
}

@Composable
private fun TrendingCard(
    trending: List<TrendingChallenge>,
    onClick: (challengeId: String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface),
    ) {
        trending.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = RuleUpTheme.colors.border)
            TrendingRow(rank = item.rank, item = item, onClick = { onClick(item.challengeId) })
        }
    }
}

@Composable
private fun TrendingRow(
    rank: Int,
    item: TrendingChallenge,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .singleClickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RankBadge(rank = rank)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
            )
            // 못 들어가는 방도 인기 목록에는 노출한다(의도된 동작) — 잠금은 색이 아니라 라벨로 알린다.
            if (!item.joinable) {
                Text(
                    text = "🔒 ${item.minTier?.value ?: "티어 제한"}",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = RuleUpTheme.colors.textMuted,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "%,d명".format(item.participantCount),
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallMedium,
            )
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val isTop = rank <= 3
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (isTop) {
                        Modifier.background(Brush.linearGradient(TopRankGradient))
                    } else {
                        Modifier.background(RuleUpTheme.colors.surfaceVariant)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            color = if (isTop) Color.White else RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}

@Composable
private fun CategoryGrid(
    categories: List<ChallengeCategoryCount>,
    onClick: (ChallengeCategoryCount) -> Unit,
) {
    if (categories.isEmpty()) {
        Text(
            text = "표시할 카테고리가 없어요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    CategoryCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = { onClick(item) },
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryCard(
    item: ChallengeCategoryCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .singleClickable(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(categoryIconRes(item.category)),
                contentDescription = null,
                tint = RuleUpTheme.colors.brand,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.name,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
            )
            Text(
                text = "%,d개 챌린지".format(item.activeGroupCount),
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun TrendingSkeleton() {
    // 카드 5개 자리를 먼저 잡아 목록이 도착할 때 화면이 튀지 않게 한다.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface),
    ) {
        repeat(TRENDING_SKELETON_COUNT) { index ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = RuleUpTheme.colors.border)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(RuleUpTheme.colors.surfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun CategoryGridSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(CATEGORY_SKELETON_COUNT / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(RuleUpTheme.colors.surfaceVariant),
                    )
                }
            }
        }
    }
}

/** 섹션 하나만 실패했을 때의 재시도 — 화면 전체를 에러로 만들지 않는다. */
@Composable
private fun SectionRetry(onRetry: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "불러오지 못했어요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
        )
        Text(
            text = "다시 시도",
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.bodyBold,
            modifier = Modifier.singleClickable(onClick = onRetry),
        )
    }
}

private const val TRENDING_SKELETON_COUNT = 5
private const val CATEGORY_SKELETON_COUNT = 12
