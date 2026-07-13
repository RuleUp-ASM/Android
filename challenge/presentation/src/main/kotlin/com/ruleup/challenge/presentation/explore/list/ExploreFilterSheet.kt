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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/**
 * 04 · 챌린지 필터 시트. 유형(그룹/솔로)·인증(자동/수동)·매너 온도 컷을 편집하고
 * "결과 보기 · N개" 로 확정한다. 조건 변경 시마다 [onPreview] 로 카운트를 미리 집계한다.
 * 매너 온도 컷은 API 정의대로 on/off(joinableOnly) — 온도 값은 서버가 토큰 사용자 기준으로 계산한다.
 * 카테고리 필터는 탐색 메인 그리드 진입 컨텍스트라 시트에서 다루지 않는다(초기화에서도 유지).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreFilterSheet(
    applied: ExploreFilter,
    previewCount: Int?,
    onPreview: (ExploreFilter) -> Unit,
    onApply: (ExploreFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(applied) }

    fun update(next: ExploreFilter) {
        draft = next
        onPreview(next)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = RuleUpTheme.colors.surface,
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            FilterSheetHeader(
                onReset = { update(ExploreFilter(category = draft.category)) },
            )
            Spacer(Modifier.height(16.dp))
            FilterSectionLabel("챌린지 유형")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterToggleButton(
                    text = "그룹",
                    selected = draft.participationType == ParticipationType.GROUP,
                    modifier = Modifier.weight(1f),
                    onClick = { update(draft.copy(participationType = draft.participationType.toggle(ParticipationType.GROUP))) },
                )
                FilterToggleButton(
                    text = "솔로",
                    selected = draft.participationType == ParticipationType.SOLO,
                    modifier = Modifier.weight(1f),
                    onClick = { update(draft.copy(participationType = draft.participationType.toggle(ParticipationType.SOLO))) },
                )
            }
            Spacer(Modifier.height(16.dp))
            FilterSectionLabel("인증 방식")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterToggleButton(
                    text = "자동 인증",
                    selected = draft.verificationMethod == SelectedMethod.AUTO,
                    modifier = Modifier.weight(1f),
                    onClick = { update(draft.copy(verificationMethod = draft.verificationMethod.toggle(SelectedMethod.AUTO))) },
                )
                FilterToggleButton(
                    text = "수동 인증",
                    selected = draft.verificationMethod == SelectedMethod.MANUAL,
                    modifier = Modifier.weight(1f),
                    onClick = { update(draft.copy(verificationMethod = draft.verificationMethod.toggle(SelectedMethod.MANUAL))) },
                )
            }
            Spacer(Modifier.height(16.dp))
            MannerCutSection(
                joinableOnly = draft.joinableOnly,
                onChange = { update(draft.copy(joinableOnly = it)) },
            )
            Spacer(Modifier.height(20.dp))
            ApplyButton(
                previewCount = previewCount,
                onClick = { onApply(draft) },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// 같은 값을 다시 누르면 해제(전체)되는 nullable 토글.
private fun <T> T?.toggle(value: T): T? = if (this == value) null else value

@Composable
private fun SheetDragHandle() {
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
private fun FilterSheetHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "필터",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.singleClickable(onClick = onReset),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⟲", color = RuleUpTheme.colors.textSecondary, fontSize = 14.sp)
            Text(
                text = "초기화",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
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
                        base.background(RuleUpGradients.Button)
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
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun MannerCutSection(
    joinableOnly: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterSectionLabel("매너 온도 컷")
            Text(
                text = if (joinableOnly) "참여 가능만" else "제한 없음",
                color = RuleUpTheme.colors.brand,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        // 온도 값은 서버가 내 매너 온도 기준으로 계산하므로(API joinableOnly) on/off 만 고른다.
        Text(
            text = "내 매너 온도로 들어갈 수 있는 챌린지만 보여요",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterToggleButton(
                text = "제한 없음",
                selected = !joinableOnly,
                modifier = Modifier.weight(1f),
                onClick = { onChange(false) },
            )
            FilterToggleButton(
                text = "참여 가능만",
                selected = joinableOnly,
                modifier = Modifier.weight(1f),
                onClick = { onChange(true) },
            )
        }
    }
}

@Composable
private fun ApplyButton(
    previewCount: Int?,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpGradients.Button)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = previewCount?.let { "결과 보기 · %,d개".format(it) } ?: "결과 보기",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
