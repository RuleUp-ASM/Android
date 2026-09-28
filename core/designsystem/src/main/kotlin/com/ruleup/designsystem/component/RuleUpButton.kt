package com.ruleup.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/** 기본 CTA 버튼. */
@Composable
fun RuleUpPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(if (enabled) RuleUpTheme.colors.brand else RuleUpTheme.colors.border)
                .singleClickable(enabled = enabled, onClick = onClick)
                .padding(CONTENT_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = RuleUpTheme.typography.cardTitle,
            color = if (enabled) Color.White else RuleUpTheme.colors.textMuted,
        )
    }
}

private val CONTENT_PADDING = PaddingValues(horizontal = 24.dp, vertical = 13.dp)

@Preview
@Composable
private fun RuleUpPrimaryButtonPreview() {
    RuleUpTheme {
        RuleUpPrimaryButton(text = "이대로 만들기")
    }
}
