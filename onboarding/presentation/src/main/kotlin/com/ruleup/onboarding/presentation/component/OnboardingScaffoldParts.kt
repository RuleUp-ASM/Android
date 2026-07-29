package com.ruleup.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/** 페이지 진행 인디케이터. [current] 만 길쭉한 그라데이션 바로 강조. */
@Composable
fun PageDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
    activeBrush: Brush = RuleUpGradients.Indicator,
    inactiveColor: Color = RuleUpTheme.colors.borderStrong,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            if (index == current) {
                Box(
                    Modifier
                        .height(6.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(activeBrush),
                )
            } else {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(inactiveColor),
                )
            }
        }
    }
}

/** 화면 하단 고정 CTA 영역. */
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
