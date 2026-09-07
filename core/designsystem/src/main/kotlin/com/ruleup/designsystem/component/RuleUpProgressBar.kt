package com.ruleup.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 진행 바. Figma `ProgressBar`(node `1177:181`).
 * @param progress 0f~1f. 벗어난 값은 잘라 낸다 — 채움이 트랙을 넘어 그려지면 레이아웃이 밀린다.
 */
@Composable
fun RuleUpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(BAR_SHAPE)
                .background(RuleUpTheme.colors.border),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(BAR_SHAPE)
                    .background(RuleUpTheme.colors.brand),
        )
    }
}

private val BAR_HEIGHT = 4.dp

// Figma 에 반경 변수가 없어 모양 토큰으로 올리지 않았다.
private val BAR_SHAPE = RoundedCornerShape(2.dp)

@Preview
@Composable
private fun RuleUpProgressBarPreview() {
    RuleUpTheme {
        RuleUpProgressBar(progress = 0.6f, modifier = Modifier.padding(12.dp))
    }
}
