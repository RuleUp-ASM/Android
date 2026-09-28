package com.ruleup.onboarding.presentation.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper

/** [LocalNavigationHelper]를 제공하는 프로필 설정 미리보기. */
@Composable
internal fun OnboardingFlowPreview(content: @Composable () -> Unit) {
    RuleUpTheme {
        com.ruleup.ui.helper.PreviewEnvironment {
            content()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        Text(
            title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
        )
        Text(
            subtitle,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
        )
    }
}

@Composable
fun InfoBox(
    background: Color,
    emoji: String,
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(background)
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 이모지라 타입 스케일이 아니라 그리는 크기로 잡는다.
        Text(emoji, fontSize = 16.sp)
        Text(text, color = textColor, style = RuleUpTheme.typography.caption)
    }
}

@Composable
fun RequirementBadge(
    required: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (required) RuleUpTheme.colors.danger else RuleUpTheme.colors.surfaceVariant
    val textColor = if (required) Color.White else RuleUpTheme.colors.textSecondary
    Box(
        modifier =
            modifier
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(background)
                .padding(horizontal = RuleUpTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (required) "필수" else "선택",
            color = textColor,
            style = RuleUpTheme.typography.micro,
        )
    }
}

@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RuleUpTheme.colors.border),
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun SectionHeaderPreview() {
    RuleUpTheme {
        SectionHeader(title = "매일 꾸준히 걷기", subtitle = "미리보기")
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun InfoBoxPreview() {
    RuleUpTheme {
        InfoBox(
            background = com.ruleup.designsystem.theme.RuleUpTheme.colors.brand,
            emoji = "✨",
            text = "꾸준히 함께해요",
            textColor = com.ruleup.designsystem.theme.RuleUpTheme.colors.brand,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RequirementBadgePreview() {
    RuleUpTheme {
        RequirementBadge(required = true)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RowDividerPreview() {
    RuleUpTheme {
        RowDivider()
    }
}
