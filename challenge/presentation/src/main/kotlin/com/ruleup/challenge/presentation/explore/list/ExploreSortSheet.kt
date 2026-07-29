package com.ruleup.challenge.presentation.explore.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

// 정렬 시트 노출 순서·라벨(Figma 05 · 챌린지 정렬 + 탐색 스펙 §3.2 7종).
private val sortOptions =
    listOf(
        SortOption(
            sort = ExploreSort.TEMPLATE_USAGE,
            title = "챌린지 참여 수",
            description = "같은 템플릿을 사용하는 전체 사용자 수",
        ),
        SortOption(
            sort = ExploreSort.PARTICIPANTS,
            title = "챌린지 참여자 수",
            description = "이 챌린지에 참여 중인 인원 수",
        ),
        SortOption(
            sort = ExploreSort.COMPLETION_RATE,
            title = "완주율 높은 순",
            description = "챌린지 완주 비율이 높은 순서",
        ),
        SortOption(
            sort = ExploreSort.SUCCESS_FAIL_RATIO,
            title = "성공/실패 비율 순",
            description = "지금 이 방의 성공 인원 비율이 높은 순서",
        ),
        SortOption(
            sort = ExploreSort.RECENT,
            title = "최근 생성 순",
            description = "가장 최근에 만들어진 챌린지 순서",
        ),
        SortOption(
            sort = ExploreSort.DEADLINE,
            title = "마감 임박 순",
            description = "모집 마감이 가까운 챌린지 순서",
        ),
        SortOption(
            sort = ExploreSort.TRENDING,
            title = "인기순",
            description = "지금 참여가 빠르게 늘고 있는 순서",
        ),
    )

private data class SortOption(
    val sort: ExploreSort,
    val title: String,
    val description: String,
)

/** 05 · 챌린지 정렬 시트. 7종 중 하나를 선택하면 즉시 적용된다(방향은 정의로 고정, 미노출). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreSortSheet(
    selected: ExploreSort,
    onSelect: (ExploreSort) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = RuleUpTheme.colors.surface,
        dragHandle = { SortSheetDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "정렬 기준",
                    color = RuleUpTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "✕",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.singleClickable(onClick = onDismiss),
                )
            }
            Spacer(Modifier.height(12.dp))
            sortOptions.forEach { option ->
                SortRow(
                    option = option,
                    selected = option.sort == selected,
                    onClick = { onSelect(option.sort) },
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SortSheetDragHandle() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(RuleUpTheme.colors.borderStrong),
        )
    }
}

@Composable
private fun SortRow(
    option: SortOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (selected) Modifier.background(RuleUpTheme.colors.brandSoft) else Modifier,
                ).singleClickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = option.title,
                color = if (selected) RuleUpTheme.colors.brandStrong else RuleUpTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
            Text(
                text = option.description,
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 11.sp,
            )
        }
        RadioMark(selected = selected)
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    if (selected) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.brand),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, RuleUpTheme.colors.borderStrong, CircleShape),
        )
    }
}
