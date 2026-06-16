package com.ruleup.onboarding.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.ruleup.onboarding.domain.ProfileAgreementPage
import com.ruleup.onboarding.presentation.profile.component.InfoBox
import com.ruleup.onboarding.presentation.profile.component.ProfileFlowPreview
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.theme.RuleUpTheme

/** 04 · 권한 안내 (4/5). 알림 등 권한 허용을 안내하는 단계. */
@Composable
fun PermissionContent(modifier: Modifier = Modifier) {
    val nav = LocalNavigationHelper.current
    ProfileSetupScaffold(
        step = 3,
        buttonText = "다음",
        modifier = modifier,
        onNext = { nav.navigateTo(ProfileAgreementPage) },
        onBack = { nav.navigateToBack() },
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

@Preview
@Composable
private fun PermissionScreenPreview() {
    ProfileFlowPreview { PermissionContent() }
}
