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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.NoOpNavigationHelper

/**
 * 프로필 설정 Content 프리뷰용 래퍼. Content 가 직접 읽는 [LocalNavigationHelper] 를 더미로 제공한다.
 */
@Composable
internal fun OnboardingFlowPreview(content: @Composable () -> Unit) {
    RuleUpTheme {
        CompositionLocalProvider(
            LocalNavigationHelper provides NoOpNavigationHelper,
        ) {
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
