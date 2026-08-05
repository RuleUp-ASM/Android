package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Gender
import com.ruleup.onboarding.domain.navigation.OnboardingPhotoPage
import com.ruleup.onboarding.domain.observability.OnboardingStep
import com.ruleup.onboarding.presentation.component.OnboardingScaffold
import com.ruleup.onboarding.presentation.onboarding.component.InfoBox
import com.ruleup.onboarding.presentation.onboarding.component.OnboardingFlowPreview
import com.ruleup.onboarding.presentation.onboarding.component.SectionHeader
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 04 · 성별.
 *
 * 화면에서는 건너뛸 수 있지만 **API 필드는 필수**다. 안 고르고 넘어가면 논바이너리로 저장된다 —
 * "안 보냄"이라는 상태가 없어서, 별도의 "응답 안 함" 항목을 두면 같은 결과를 두 갈래로 표현하게
 * 된다. 그래서 카드 해제 상태가 곧 건너뛰기다.
 */
@Composable
fun GenderContent(
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
    gender: Gender? = null,
) {
    val nav = LocalNavigationHelper.current
    OnboardingScaffold(
        step = OnboardingStep.GENDER,
        skipped = gender == null,
        buttonText = "다음",
        modifier = modifier,
        onNext = { nav.navigateTo(OnboardingPhotoPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "성별을 알려주세요",
            subtitle = "통계에만 사용해요",
            titleSize = 22,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GenderCard(
                glyph = "♂",
                label = "남성",
                selected = gender == Gender.MALE,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(OnboardingIntent.SetGender(Gender.MALE)) },
            )
            GenderCard(
                glyph = "♀",
                label = "여성",
                selected = gender == Gender.FEMALE,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(OnboardingIntent.SetGender(Gender.FEMALE)) },
            )
        }

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "ℹ️",
            text = "고르지 않고 넘어가도 괜찮아요. 다른 사용자에게 공개되지 않아요",
            textColor = RuleUpTheme.colors.brandStrong,
        )
    }
}

@Composable
private fun GenderCard(
    glyph: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .height(93.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (selected) {
                        Modifier.background(RuleUpTheme.colors.brandSoft)
                    } else {
                        Modifier.background(RuleUpTheme.colors.surface)
                    },
                ).border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) RuleUpTheme.colors.brand else RuleUpTheme.colors.border,
                    shape = RoundedCornerShape(12.dp),
                ).singleClickable(globalGuard = false, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            glyph,
            color = if (selected) RuleUpTheme.colors.brand else RuleUpTheme.colors.textSecondary,
            fontSize = 28.sp,
        )
        Text(
            label,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Preview
@Composable
private fun GenderScreenPreview() {
    OnboardingFlowPreview { GenderContent(onIntent = {}, gender = Gender.MALE) }
}
