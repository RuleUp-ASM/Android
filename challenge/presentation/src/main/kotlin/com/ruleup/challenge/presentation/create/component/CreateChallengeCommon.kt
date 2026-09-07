package com.ruleup.challenge.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

@Composable
fun CreateChallengeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    trailingText: String? = null,
    onTrailingClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                modifier = Modifier.singleClickable(onClick = onBack),
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.title,
            )
            Text(
                title,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.section,
            )
        }
        if (trailingText != null) {
            Text(
                trailingText,
                modifier = Modifier.singleClickable(onClick = onTrailingClick),
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.captionBold,
        )
        trailing()
    }
}

@Composable
fun SmallBadge(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = textColor,
            style = RuleUpTheme.typography.micro,
        )
    }
}

/** 이름과 달리 그라데이션이 아니다 — Figma 에 그라데이션 토큰이 없어 단색 brand 로 칠한다. */
@Composable
fun GradientSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .width(44.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .let { base ->
                    if (checked) {
                        base.background(RuleUpTheme.colors.brand)
                    } else {
                        base.background(RuleUpTheme.colors.borderStrong)
                    }
                }.singleClickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
        )
    }
}

@Composable
fun InfoNote(
    emoji: String,
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(background)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = RuleUpTheme.typography.body)
        Text(text, color = textColor, style = RuleUpTheme.typography.caption)
    }
}
