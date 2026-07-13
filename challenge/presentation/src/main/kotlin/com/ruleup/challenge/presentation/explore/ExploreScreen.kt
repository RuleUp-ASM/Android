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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreIntent
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreState
import com.ruleup.challenge.presentation.explore.viewmodel.ExploreViewModel
import com.ruleup.ui.R
import com.ruleup.ui.category.categoryIconRes
import com.ruleup.ui.component.RuleUpBottomTab
import com.ruleup.ui.component.RuleUpBottomTabBar
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme

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
    ExploreContent(modifier = modifier, state = state, onIntent = viewModel::onIntent)
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
            when {
                state.isLoading -> LoadingBox()
                state.errorMessage != null ->
                    ErrorBox(message = state.errorMessage, onRetry = { onIntent(ExploreIntent.Load) })

                else -> {
                    SectionHeader(title = "실시간 인기", onSeeAll = { onIntent(ExploreIntent.OpenTrendingAll) })
                    TrendingCard(
                        trending = state.trending,
                        onClick = { onIntent(ExploreIntent.OpenChallenge(it)) },
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(title = "카테고리 탐색", onSeeAll = { onIntent(ExploreIntent.OpenCategoryAll) })
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
        }
        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.CHALLENGE,
            onTabClick = { tab ->
                if (tab == RuleUpBottomTab.HOME) onIntent(ExploreIntent.OpenHome)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "전체 ›",
            color = RuleUpTheme.colors.brand,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
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
        if (trending.isEmpty()) {
            Text(
                text = "아직 인기 챌린지가 없어요",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
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
        Text(
            text = item.title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
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
            fontSize = 12.sp,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "%,d개 챌린지".format(item.activeChallengeCount),
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
    }
}

@Composable
private fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 13.sp,
        )
        Text(
            text = "다시 시도",
            color = RuleUpTheme.colors.brand,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.singleClickable(onClick = onRetry),
        )
    }
}
