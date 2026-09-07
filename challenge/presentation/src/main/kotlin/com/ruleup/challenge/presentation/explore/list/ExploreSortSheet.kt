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
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

// 정렬 시트 노출 순서·라벨(명세 6종). 기본값인 인기순을 맨 위에 둔다.
private val sortOptions =
    listOf(
        SortOption(
            sort = ExploreSort.POPULAR,
            title = "인기순",
            description = "최근 24시간 동안 참여가 많이 붙은 순서",
        ),
        SortOption(
            sort = ExploreSort.PARTICIPANTS,
            title = "참여자 많은 순",
            description = "지금 이 방에 참여 중인 인원이 많은 순서",
        ),
        SortOption(
            sort = ExploreSort.COMPLETION_RATE,
            title = "완주율 높은 순",
            description = "기록이 충분한 방만 보여요",
        ),
        SortOption(
            sort = ExploreSort.SUCCESS_FAIL_RATIO,
            title = "유지율 높은 순",
            description = "기록이 충분한 방만 보여요",
        ),
        SortOption(
            sort = ExploreSort.RECENT,
            title = "최근 생성 순",
            description = "가장 최근에 만들어진 챌린지 순서",
        ),
        SortOption(
            sort = ExploreSort.DEADLINE,
            title = "마감 임박 순",
            description = "곧 끝나는 챌린지 순서",
        ),
    )

private data class SortOption(
    val sort: ExploreSort,
    val title: String,
    val description: String,
)

/** 05 · 챌린지 정렬 시트. 명세 6종 중 하나를 고르면 즉시 적용된다(방향은 정의로 고정, 미노출). */
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
                    style = RuleUpTheme.typography.section,
                )
                Text(
                    text = "✕",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.section,
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
                style = if (selected) RuleUpTheme.typography.cardTitle else RuleUpTheme.typography.labelMedium,
            )
            Text(
                text = option.description,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.caption,
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
            Text(text = "✓", color = Color.White, style = RuleUpTheme.typography.smallBold)
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
