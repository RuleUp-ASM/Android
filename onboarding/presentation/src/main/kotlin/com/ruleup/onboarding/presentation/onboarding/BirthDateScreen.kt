package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.domain.navigation.OnboardingGenderPage
import com.ruleup.onboarding.domain.observability.OnboardingStep
import com.ruleup.onboarding.presentation.component.OnboardingScaffold
import com.ruleup.onboarding.presentation.onboarding.component.InfoBox
import com.ruleup.onboarding.presentation.onboarding.component.OnboardingFlowPreview
import com.ruleup.onboarding.presentation.onboarding.component.SectionHeader
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 03 · 생일. **필수**이고 만 14세 미만은 가입할 수 없다(법적 요구사항). 성별은 화면에서
 * 건너뛸 수 있지만 API 필드는 필수라, 안 고르면 논바이너리로 저장된다.
 *
 * 생일이 유효해야 "다음"으로 넘어간다 — 여기서 막지 않으면 약관까지 다 채운 뒤 마지막 제출에서
 * `BIRTHDATE_UNDERAGE` 로 튕긴다.
 */
@Composable
fun BirthDateContent(
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
    birthDateInput: String = "",
    birthDateError: String? = null,
    birthDateValid: Boolean = false,
) {
    val nav = LocalNavigationHelper.current
    OnboardingScaffold(
        step = OnboardingStep.BIRTH,
        buttonText = "다음",
        modifier = modifier,
        nextEnabled = birthDateValid,
        onNext = { nav.navigateTo(OnboardingGenderPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "생일이 언제예요?",
            subtitle = "가입 조건 확인에만 사용해요",
        )

        BirthDateSection(
            digits = birthDateInput,
            error = birthDateError,
            onChange = { onIntent(OnboardingIntent.SetBirthDate(it)) },
        )

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "ℹ️",
            text = "만 14세 이상만 가입할 수 있어요. 생일은 가입 후 수정할 수 없어요",
            textColor = RuleUpTheme.colors.brandStrong,
        )
    }
}

/** 생일 입력: YYYY/MM/DD 자동 포맷 + 검증 실패 사유 인라인 표시. */
@Composable
private fun BirthDateSection(
    digits: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("생일", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
            Text("YYYY / MM / DD", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.small)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(
                        1.dp,
                        if (error != null) RuleUpTheme.colors.danger else RuleUpTheme.colors.border,
                        RoundedCornerShape(12.dp),
                    ).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = digits,
                onValueChange = { input -> onChange(input.filter { it.isDigit() }.take(8)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle =
                    RuleUpTheme.typography.numberS.copy(color = RuleUpTheme.colors.textPrimary),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                decorationBox = { inner ->
                    if (digits.isEmpty()) {
                        Text("1999 / 03 / 15", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.numberS)
                    }
                    inner()
                },
            )
        }
        if (error != null) {
            Text(error, color = RuleUpTheme.colors.danger, style = RuleUpTheme.typography.small)
        }
    }
}

@Preview
@Composable
private fun BirthDateScreenPreview() {
    OnboardingFlowPreview {
        BirthDateContent(onIntent = {}, birthDateInput = "19990315", birthDateValid = true)
    }
}
