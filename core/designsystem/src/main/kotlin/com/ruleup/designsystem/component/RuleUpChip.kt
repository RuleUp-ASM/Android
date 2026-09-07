package com.ruleup.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * [StatusChip] 의 색조. Figma 가 정의한 네 가지가 전부다.
 * 배경·글자색 조합을 여기서 닫아 둔다 — 화면이 각자 고르면 성공 배경에 위험 글자색이 나온다.
 */
enum class StatusChipTone {
    Success,
    Danger,
    Warning,
    Info,
}

/** 상태 뱃지. Figma `StatusChip/…`(node `1177:165`). */
@Composable
fun StatusChip(
    text: String,
    tone: StatusChipTone,
    modifier: Modifier = Modifier,
) {
    val colors = RuleUpTheme.colors
    val background =
        when (tone) {
            StatusChipTone.Success -> colors.successContainer
            StatusChipTone.Danger -> colors.dangerContainer
            StatusChipTone.Warning -> colors.warningContainer
            StatusChipTone.Info -> colors.brandSoft
        }
    val content =
        when (tone) {
            StatusChipTone.Success -> colors.success
            StatusChipTone.Danger -> colors.danger
            StatusChipTone.Warning -> colors.warning
            StatusChipTone.Info -> colors.brand
        }
    Text(
        text = text,
        style = RuleUpTheme.typography.tinyBold,
        color = content,
        modifier =
            modifier
                // Figma 에 반경 변수가 없어 모양 토큰으로 올리지 않았다.
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 필터 칩. Figma `FilterChip/Selected`·`FilterChip/Default`(node `1177:174`).
 * 선택 상태에 테두리·굵기까지 바꾸는 이유 — 색만으로 구분하면 색각 이상 사용자가 못 읽는다.
 */
@Composable
fun RuleUpFilterChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = RuleUpTheme.colors
    Row(
        modifier =
            modifier
                .clip(RuleUpTheme.shapes.large)
                .background(if (selected) colors.brandSoft else colors.surface)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) colors.brand else colors.border,
                    shape = RuleUpTheme.shapes.large,
                ).singleClickable(globalGuard = false, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            style = if (selected) RuleUpTheme.typography.smallBold else RuleUpTheme.typography.small,
            color = if (selected) colors.brand else colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun StatusChipPreview() {
    RuleUpTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusChip(text = "성공", tone = StatusChipTone.Success)
            StatusChip(text = "실패", tone = StatusChipTone.Danger)
            StatusChip(text = "실패 위험", tone = StatusChipTone.Warning)
            StatusChip(text = "통계 반영 예정", tone = StatusChipTone.Info)
        }
    }
}

@Preview
@Composable
private fun RuleUpFilterChipPreview() {
    RuleUpTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.background(Color.White).padding(12.dp),
        ) {
            RuleUpFilterChip(text = "선택됨", selected = true)
            RuleUpFilterChip(text = "기본", selected = false)
        }
    }
}
