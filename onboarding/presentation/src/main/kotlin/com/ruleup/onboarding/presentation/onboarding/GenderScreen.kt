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
 * **필수 입력이다** (회원 정책 §2). 고르기 전까지 다음 버튼을 잠근다 — 건너뛰기를 열어 두면
 * "안 고름"을 저장할 값이 없어 결국 없는 성별을 지어내게 된다.
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
        buttonText = "다음",
        modifier = modifier,
        nextEnabled = gender != null,
        onNext = { nav.navigateTo(OnboardingPhotoPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "성별을 알려주세요",
            subtitle = "통계에만 사용해요",
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
            text = "다른 사용자에게 공개되지 않아요",
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
            // 기호 그림이라 타입 스케일이 아니라 그리는 크기로 잡는다.
            fontSize = 28.sp,
        )
        Text(
            label,
            color = RuleUpTheme.colors.textPrimary,
            style = if (selected) RuleUpTheme.typography.cardTitle else RuleUpTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
private fun GenderScreenPreview() {
    OnboardingFlowPreview { GenderContent(onIntent = {}, gender = Gender.MALE) }
}
