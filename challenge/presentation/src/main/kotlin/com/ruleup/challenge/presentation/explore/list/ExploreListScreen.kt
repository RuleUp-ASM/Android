package com.ruleup.challenge.presentation.explore.list

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ExploreChallenge
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.presentation.explore.list.viewmodel.ExploreListIntent
import com.ruleup.challenge.presentation.explore.list.viewmodel.ExploreListState
import com.ruleup.challenge.presentation.explore.list.viewmodel.ExploreListViewModel
import com.ruleup.designsystem.R
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryIconRes
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 마감 여유가 이 값 이하면 D-day 배지를 위험색으로 강조한다.
private const val DDAY_URGENT_THRESHOLD = 7L

// 다음 페이지 프리페치를 시작할 하단 잔여 아이템 수.
private const val LOAD_MORE_PREFETCH = 3

/** 챌린지 둘러보기(Figma 02 · 챌린지 둘러보기). 필터(AND) + 정렬 7종 + 커서 무한 스크롤. */
@Composable
fun ExploreListScreen(
    category: String?,
    sort: String?,
    modifier: Modifier = Modifier,
    viewModel: ExploreListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(ExploreListIntent.Load(category = category, sort = sort)) }
    ExploreListContent(modifier = modifier, state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun ExploreListContent(
    state: ExploreListState,
    onIntent: (ExploreListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        ListHeader(onBack = { onIntent(ExploreListIntent.Back) })
        TotalCountLabel(totalCount = state.totalCount)
        Spacer(Modifier.height(10.dp))
        FilterSortRow(
            filterCount = state.filter.copy(category = null).activeCount,
            sortLabel = state.sort.shortLabel,
            onFilterClick = { showFilterSheet = true },
            onSortClick = { showSortSheet = true },
        )
        Spacer(Modifier.height(4.dp))
        ChallengeList(state = state, onIntent = onIntent)
    }

    if (showFilterSheet) {
        ExploreFilterSheet(
            applied = state.filter,
            previewCount = state.previewCount,
            onPreview = { onIntent(ExploreListIntent.PreviewFilter(it)) },
            onApply = {
                showFilterSheet = false
                onIntent(ExploreListIntent.ApplyFilter(it))
            },
            onDismiss = { showFilterSheet = false },
        )
    }
    if (showSortSheet) {
        ExploreSortSheet(
            selected = state.sort,
            onSelect = {
                showSortSheet = false
                onIntent(ExploreListIntent.SelectSort(it))
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

@Composable
private fun ListHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "뒤로",
            tint = RuleUpTheme.colors.textPrimary,
            modifier =
                Modifier
                    .size(24.dp)
                    .singleClickable(onClick = onBack),
        )
        Text(
            text = "챌린지 둘러보기",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
    }
}

@Composable
private fun TotalCountLabel(totalCount: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "전체", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
        Text(
            text = "%,d".format(totalCount),
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.bodyBold,
        )
        Text(text = "개", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
    }
}

@Composable
private fun FilterSortRow(
    filterCount: Int,
    sortLabel: String,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 필터 칩: 적용 중인 조건 수를 배지로 노출
        Row(
            modifier =
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(RuleUpTheme.colors.brandSoft)
                    .border(1.dp, RuleUpTheme.colors.brand, RoundedCornerShape(18.dp))
                    .singleClickable(onClick = onFilterClick)
                    .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "필터",
                color = RuleUpTheme.colors.brandStrong,
                style = RuleUpTheme.typography.bodyBold,
            )
            if (filterCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(RuleUpTheme.colors.brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = filterCount.toString(),
                        color = Color.White,
                        style = RuleUpTheme.typography.tinyBold,
                    )
                }
            }
        }
        // 정렬 칩: 현재 정렬 라벨 + 시트 열기
        Row(
            modifier =
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(18.dp))
                    .singleClickable(onClick = onSortClick)
                    .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = sortLabel,
                color = RuleUpTheme.colors.textSlate,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(text = "⌄", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
        }
    }
}

@Composable
private fun ChallengeList(
    state: ExploreListState,
    onIntent: (ExploreListIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    // 하단 근접(잔여 LOAD_MORE_PREFETCH 미만) 시 다음 커서 페이지를 미리 요청한다.
    val shouldLoadMore by remember(state.canLoadMore) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.canLoadMore && lastVisible >= info.totalItemsCount - LOAD_MORE_PREFETCH
        }
    }
    // 이펙트 재시작 없이 최신 onIntent 를 참조한다(compose:lambda-param-in-effect).
    val currentOnIntent by rememberUpdatedState(onIntent)
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) currentOnIntent(ExploreListIntent.LoadMore)
    }

    when {
        state.isLoading -> CenterBox { CircularProgressIndicator(color = RuleUpTheme.colors.brand) }
        state.errorMessage != null && state.challenges.isEmpty() ->
            CenterBox {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = state.errorMessage, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
                    Text(
                        text = "다시 시도",
                        color = RuleUpTheme.colors.brand,
                        style = RuleUpTheme.typography.bodyBold,
                        modifier =
                            Modifier.singleClickable {
                                onIntent(ExploreListIntent.ApplyFilter(state.filter))
                            },
                    )
                }
            }

        state.challenges.isEmpty() ->
            CenterBox {
                Text(
                    text = "조건에 맞는 챌린지가 없어요",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.body,
                )
            }

        else ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(state.challenges, key = { _, item -> item.challengeId }) { _, item ->
                    ExploreChallengeCard(
                        item = item,
                        sort = state.sort,
                        onClick = { onIntent(ExploreListIntent.OpenChallenge(item.challengeId)) },
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = RuleUpTheme.colors.brand,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ExploreChallengeCard(
    item: ExploreChallenge,
    sort: ExploreSort,
    onClick: () -> Unit,
) {
    val accent = categoryAccentColor(item.category)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .singleClickable(onClick = onClick)
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(categoryIconRes(item.category)),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.cardTitle,
                    modifier = Modifier.weight(1f),
                )
                DdayBadge(endDate = item.endDate)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagChip(
                    text = if (item.participationType == ParticipationType.GROUP) "그룹" else "솔로",
                    background = RuleUpTheme.colors.surfaceVariant,
                    textColor = RuleUpTheme.colors.textSlate,
                )
                if (item.verificationMethod == SelectedMethod.AUTO) {
                    TagChip(
                        text = "자동인증",
                        // 자동인증 표식의 파랑은 Figma 팔레트 15색에 없다. 화면 디자인에서 온 값이라 남긴다.
                        background = Color(0xFF3B82F6).copy(alpha = 0.12f),
                        textColor = Color(0xFF2563EB),
                    )
                } else {
                    TagChip(
                        text = "수동인증",
                        background = RuleUpTheme.colors.surfaceVariant,
                        textColor = RuleUpTheme.colors.textSlate,
                    )
                }
            }
            CardStats(item = item, sort = sort)
        }
    }
}

/**
 * 카드 하단 지표 라인: 참여자 · 완주(템플릿 완주율) · 템플릿(사용자 수).
 * 활성 정렬이 완주율/성공·실패 비율인데 표본 부족(null)이면 스펙 안내 문구로 대체한다.
 */
@Composable
private fun CardStats(
    item: ExploreChallenge,
    sort: ExploreSort,
) {
    val needsSampleNotice =
        (sort == ExploreSort.COMPLETION_RATE && item.completionRate == null) ||
            (sort == ExploreSort.SUCCESS_FAIL_RATIO && item.successRate == null)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatsLine(item = item, sort = sort)
        if (needsSampleNotice) {
            Text(
                text = "아직 참여자가 적어 값을 낼 수 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun StatsLine(
    item: ExploreChallenge,
    sort: ExploreSort,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "참여자 %,d".format(item.participantCount),
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallBold,
        )
        val successRate = item.successRate
        val completionRate = item.completionRate
        when {
            sort == ExploreSort.SUCCESS_FAIL_RATIO && successRate != null -> {
                Dot()
                Text(
                    text = "성공률 ${(successRate * 100).toInt()}%",
                    color = RuleUpTheme.colors.warning,
                    style = RuleUpTheme.typography.smallBold,
                )
            }

            completionRate != null -> {
                Dot()
                Text(
                    text = "완주 ${(completionRate * 100).toInt()}%",
                    color = RuleUpTheme.colors.warning,
                    style = RuleUpTheme.typography.smallBold,
                )
            }
        }
        Dot()
        Text(
            text = "템플릿 ${compactCount(item.templateUsageCount)}",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.small,
        )
    }
}

@Composable
private fun Dot() {
    Text(text = "·", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.small)
}

@Composable
private fun DdayBadge(endDate: String?) {
    val dday = remember(endDate) { ddayOf(endDate) }
    val (text, color) =
        when {
            dday == null -> "상시" to RuleUpTheme.colors.textSlate
            dday <= DDAY_URGENT_THRESHOLD -> "D-$dday" to RuleUpTheme.colors.danger
            else -> "D-$dday" to RuleUpTheme.colors.warning
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (dday == null) RuleUpTheme.colors.surfaceVariant else color.copy(alpha = 0.12f),
                ).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = RuleUpTheme.typography.tinyBold,
        )
    }
}

@Composable
private fun TagChip(
    text: String,
    background: Color,
    textColor: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = RuleUpTheme.typography.tinyBold,
        )
    }
}

/** 종료일까지 남은 일수. 파싱 불가/없음(상시)은 null, 지난 날짜는 0. */
private fun ddayOf(endDate: String?): Long? {
    if (endDate.isNullOrBlank()) return null
    return runCatching {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(endDate)).coerceAtLeast(0)
    }.getOrNull()
}

/** 1,000 이상은 "5.6k" 압축 표기(Figma 카드 "템플릿 5.6k"). */
private fun compactCount(count: Int): String =
    if (count >= 1000) {
        val k = count / 1000.0
        if (k >= 10) "${k.toInt()}k" else "%.1fk".format(k)
    } else {
        "%,d".format(count)
    }

/** 정렬 칩에 노출하는 짧은 라벨. */
internal val ExploreSort.shortLabel: String
    get() =
        when (this) {
            ExploreSort.TEMPLATE_USAGE -> "참여 수"
            ExploreSort.PARTICIPANTS -> "참여자 수"
            ExploreSort.COMPLETION_RATE -> "완주율"
            ExploreSort.SUCCESS_FAIL_RATIO -> "성공/실패"
            ExploreSort.RECENT -> "최근 생성"
            ExploreSort.DEADLINE -> "마감 임박"
            ExploreSort.TRENDING -> "인기순"
        }
