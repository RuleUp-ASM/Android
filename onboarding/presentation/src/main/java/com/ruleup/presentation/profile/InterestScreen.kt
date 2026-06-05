package com.ruleup.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.entity.user.InterestCategory
import com.ruleup.presentation.profile.component.InfoBox
import com.ruleup.presentation.profile.component.SectionHeader
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.theme.RuleUpColors
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/** 03 · 관심 분야 (3/4). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestContent(
    modifier: Modifier = Modifier,
    selected: List<InterestCategory> = listOf(InterestCategory.EXERCISE),
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onClick: (InterestCategory) -> Unit = {},
) {
    ProfileSetupScaffold(
        step = 2,
        buttonText = "다음",
        modifier = modifier,
        onNext = onNext,
        onBack = onBack,
    ) {
        SectionHeader(
            title = "어떤 챌린지에 관심 있나요?",
            subtitle = "선택한 분야 기반으로 챌린지를 추천해드려요",
        )

        // 관심 분야 칩
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
        ) {
            InterestCategory.entries.forEach { interest ->
                InterestChip(interest = interest, selected = interest in selected, onClick = { onClick(it) })
            }
        }

        InfoBox(
            background = RuleUpColors.PurpleTint,
            emoji = "🤖",
            text = "선택한 분야는 언제든 마이페이지에서 수정할 수 있어요",
            textColor = RuleUpTheme.colors.textSlate,
        )
    }
}

@Composable
private fun InterestChip(
    interest: InterestCategory,
    selected: Boolean,
    onClick: (InterestCategory) -> Unit,
) {
    val base =
        Modifier
            .height(42.dp)
            .clip(RuleUpTheme.shapes.chip)
    val styled =
        if (selected) {
            base.background(RuleUpGradients.Button)
        } else {
            base
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.chip)
        }
    Row(
        modifier =
            styled.padding(horizontal = RuleUpTheme.spacing.lg).clickable {
                onClick(interest)
            },
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(interest.emoji, fontSize = 16.sp)
        Text(
            interest.label,
            color = if (selected) Color.White else RuleUpTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun InterestScreenPreview() {
    RuleUpTheme { InterestContent() }
}
