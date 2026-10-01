package com.ruleup.challenge.presentation.explore.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category

/** 04 · 챌린지 필터 시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreFilterSheet(
    applied: ExploreFilter,
    onApply: (ExploreFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 시트에서는 선택만 하고 "적용"을 눌렀을 때 1회 조회한다
    var draft by remember { mutableStateOf(applied) }

    fun update(next: ExploreFilter) {
        draft = next
    }

    // 반만 펼친 상태에서는 적용 버튼이 가려져 처음부터 끝까지 펼친다.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = RuleUpTheme.colors.surface,
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
        ) {
            FilterSheetHeader(
                onReset = { update(ExploreFilter.none) },
            )
            Spacer(Modifier.height(16.dp))
            CategorySection(
                selected = draft.categories,
                onToggle = { category -> update(draft.copy(categories = draft.categories.toggleMember(category))) },
            )
            Spacer(Modifier.height(16.dp))
            FilterSectionLabel("인증 방식")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterToggleButton(
                    text = "자동 인증",
                    selected = draft.verifyType?.isAuto == true,
                    modifier = Modifier.weight(1f),
                    onClick = { update(draft.copy(verifyType = draft.verifyType.toggle(VerificationType.AUTO))) },
                )
                FilterToggleButton(
                    text = "직접 체크",
                    selected = draft.verifyType?.isAuto == false,
                    modifier = Modifier.weight(1f),
                    onClick = { update(draft.copy(verifyType = draft.verifyType.toggle(VerificationType.MANUAL))) },
                )
            }
            Spacer(Modifier.height(16.dp))
            TierCutSection(
                eligibleOnly = draft.eligibleOnly,
                onChange = { update(draft.copy(eligibleOnly = it)) },
            )
            Spacer(Modifier.height(20.dp))
            ApplyButton(onClick = { onApply(draft) })
            Spacer(Modifier.height(20.dp))
        }
    }
}

private const val CATEGORY_COLUMNS = 3

// 같은 값을 다시 누르면 해제(전체)되는 nullable 토글.
private fun <T> T?.toggle(value: T): T? = if (this == value) null else value

/** 카테고리는 복수 선택이라 집합에서 넣고 뺀다. */
private fun Set<Category>.toggleMember(value: Category): Set<Category> = if (value in this) this - value else this + value

/** 카테고리 12종 복수 선택. 가장 긴 이름(커리어·생산성)이 들어가는 3열 등폭 격자다. */
@Composable
private fun CategorySection(
    selected: Set<Category>,
    onToggle: (Category) -> Unit,
) {
    Column {
        FilterSectionLabel("카테고리")
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = CATEGORY_COLUMNS,
        ) {
            Category.entries.forEach { category ->
                FilterToggleButton(
                    text = category.label,
                    selected = category in selected,
                    onClick = { onToggle(category) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    // Figma 시트 공통 핸들 규격(1134:682)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RuleUpTheme.colors.borderStrong),
        )
    }
}

@Composable
private fun FilterSheetHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "필터",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
        Row(
            modifier = Modifier.singleClickable(onClick = onReset),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⟲", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.labelMedium)
            Text(
                text = "초기화",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.cardTitle,
    )
}

@Composable
private fun FilterToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .let { base ->
                    if (selected) {
                        base.background(RuleUpTheme.colors.brand)
                    } else {
                        base
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(12.dp))
                    }
                }.singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else RuleUpTheme.colors.textSlate,
            style = if (selected) RuleUpTheme.typography.cardTitle else RuleUpTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun TierCutSection(
    eligibleOnly: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterSectionLabel("티어 컷")
            Text(
                text = if (eligibleOnly) "참여 가능만" else "제한 없음",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        // 티어 값은 서버가 내 표시 티어 기준으로 계산하므로(API eligibleOnly) on/off 만 고른다.
        Text(
            text = "내 티어로 들어갈 수 있는 챌린지만 보여요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterToggleButton(
                text = "제한 없음",
                selected = !eligibleOnly,
                modifier = Modifier.weight(1f),
                onClick = { onChange(false) },
            )
            FilterToggleButton(
                text = "참여 가능만",
                selected = eligibleOnly,
                modifier = Modifier.weight(1f),
                onClick = { onChange(true) },
            )
        }
    }
}

@Composable
private fun ApplyButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.brand)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "적용",
            color = Color.White,
            style = RuleUpTheme.typography.cardTitle,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ExploreFilterSheetPreview() {
    RuleUpTheme {
        ExploreFilterSheet(
            applied =
                com.ruleup.challenge.domain.entity
                    .ExploreFilter(),
            onApply = { },
            onDismiss = { },
        )
    }
}
