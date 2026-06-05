package com.ruleup.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ruleup.presentation.profile.component.InfoBox
import com.ruleup.presentation.profile.component.SectionHeader
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.theme.RuleUpTheme

/** 04 · 권한 안내 (4/5). 알림 등 권한 허용을 안내하는 단계. */
@Composable
fun PermissionContent(
    modifier: Modifier = Modifier,
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    ProfileSetupScaffold(
        step = 3,
        buttonText = "다음",
        modifier = modifier,
        onNext = onNext,
        onBack = onBack,
    ) {
        SectionHeader(
            title = "권한을 허용해주세요",
            subtitle = "원활한 사용을 위해 권한이 필요해요",
            titleSize = 22,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
        ) {
            Text(
                "🔔 알림 — 챌린지 리마인더와 활동 소식을 받아볼 수 있어요",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.body,
                textAlign = TextAlign.Start,
            )
        }

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "🔒",
            text = "권한은 나중에 설정에서 언제든 변경할 수 있어요",
            textColor = RuleUpTheme.colors.brandStrong,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun PermissionScreenPreview() {
    RuleUpTheme { PermissionContent() }
}
