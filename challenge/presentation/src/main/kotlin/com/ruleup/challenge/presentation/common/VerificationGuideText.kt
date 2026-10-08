package com.ruleup.challenge.presentation.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruleup.designsystem.theme.RuleUpTheme

/** 인증 방법 안내(서버가 준 verification.guide). 서버가 아직 채우는 중이면 그렇다고 알린다. */
@Composable
internal fun VerificationGuideText(
    guide: String?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = guide ?: "아직 입력중입니다.",
        color = if (guide != null) RuleUpTheme.colors.textSlate else RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.body,
        modifier = modifier,
    )
}
