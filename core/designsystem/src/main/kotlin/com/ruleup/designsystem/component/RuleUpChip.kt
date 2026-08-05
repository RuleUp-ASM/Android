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
 *
 * 화면이 배경색·글자색을 각자 고르지 않도록 조합을 여기서 닫아 둔다 — 성공 배경에 위험 글자색
 * 같은 조합이 나올 여지를 없앤다.
 */
enum class StatusChipTone {
    /** 인증 성공. */
    Success,

    /** 인증 실패. */
    Danger,

    /** 실패 위험(마감 임박 등). */
    Warning,

    /** 안내(통계 반영 예정 등). */
    Info,
}

/**
 * 상태 뱃지. Figma `StatusChip/…`(node `1177:165`).
 *
 * 반경 8, 좌우 8 · 상하 3 패딩, 라벨은 `Tiny Bold`(10 Bold)다.
 */
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
                // Figma 컴포넌트 반경. 모양 토큰에는 8이 없다 — Figma 에 아직 반경 변수가 없어
                // 토큰으로 올리지 않았다.
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 필터 칩. Figma `FilterChip/Selected`·`FilterChip/Default`(node `1177:174`).
 *
 * 반경 14, 좌우 12 · 상하 6 패딩. 선택 상태는 테두리가 1.5 로 굵어지고 라벨이 Bold 가 된다 —
 * 색만으로 구분하면 색각 이상 사용자가 선택 여부를 읽지 못한다.
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
