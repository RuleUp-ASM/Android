package com.ruleup.onboarding.presentation.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Gender
import com.ruleup.onboarding.domain.navigation.ProfileAgreementPage
import com.ruleup.onboarding.presentation.component.ProfileSetupScaffold
import com.ruleup.onboarding.presentation.profile.component.InfoBox
import com.ruleup.onboarding.presentation.profile.component.ProfileFlowPreview
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 05 · 생일·성별. 생일은 **필수**이고 만 14세 미만은 가입할 수 없다(법적 요구사항). 성별은 화면에서
 * 건너뛸 수 있지만 API 필드는 필수라, 안 고르면 논바이너리로 저장된다.
 *
 * 생일이 유효해야 "다음"으로 넘어간다 — 여기서 막지 않으면 약관까지 다 채운 뒤 마지막 제출에서
 * `BIRTHDATE_UNDERAGE` 로 튕긴다.
 */
@Composable
fun BasicInfoContent(
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    birthDateInput: String = "",
    birthDateError: String? = null,
    birthDateValid: Boolean = false,
    gender: Gender? = null,
) {
    val nav = LocalNavigationHelper.current
    ProfileSetupScaffold(
        step = 4,
        buttonText = "다음",
        modifier = modifier,
        nextEnabled = birthDateValid,
        onNext = { nav.navigateTo(ProfileAgreementPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "생일과 성별을 알려주세요",
            subtitle = "생일은 가입 조건 확인에만 사용해요",
            titleSize = 22,
        )

        BirthDateSection(
            digits = birthDateInput,
            error = birthDateError,
            onChange = { onIntent(ProfileIntent.SetBirthDate(it)) },
        )

        GenderSection(
            gender = gender,
            onSelect = { onIntent(ProfileIntent.SetGender(it)) },
        )

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "ℹ️",
            text = "생일은 가입 후 수정할 수 없어요. 성별은 통계에만 쓰이고 다른 사용자에게 공개되지 않아요",
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
            Text("생일", color = RuleUpTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("YYYY / MM / DD", color = RuleUpTheme.colors.textSecondary, fontSize = 12.sp)
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
                    TextStyle(
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                cursorBrush = RuleUpGradients.Button,
                decorationBox = { inner ->
                    if (digits.isEmpty()) {
                        Text("1999 / 03 / 15", color = RuleUpTheme.colors.textSecondary, fontSize = 16.sp)
                    }
                    inner()
                },
            )
        }
        if (error != null) {
            Text(error, color = RuleUpTheme.colors.danger, fontSize = 12.sp)
        }
    }
}

/**
 * 성별 선택: 남성/여성 카드. 같은 카드를 다시 누르면 해제되고, 해제 상태로 넘어가면 서버에는
 * 논바이너리로 저장된다 — 화면에 "응답 안 함" 항목을 따로 두지 않는 이유다.
 */
@Composable
private fun GenderSection(
    gender: Gender?,
    onSelect: (Gender) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("성별", color = RuleUpTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GenderCard(
                glyph = "♂",
                label = "남성",
                selected = gender == Gender.MALE,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(Gender.MALE) },
            )
            GenderCard(
                glyph = "♀",
                label = "여성",
                selected = gender == Gender.FEMALE,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(Gender.FEMALE) },
            )
        }
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
                .height(92.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (selected) {
                        Modifier.background(RuleUpGradients.Button)
                    } else {
                        Modifier
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(12.dp))
                    },
                ).singleClickable(globalGuard = false) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val contentColor = if (selected) Color.White else RuleUpTheme.colors.textPrimary
        Text(glyph, color = contentColor, fontSize = 26.sp)
        Text(
            label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Preview
@Composable
private fun BasicInfoScreenPreview() {
    ProfileFlowPreview {
        BasicInfoContent(onIntent = {}, birthDateInput = "19990315", birthDateValid = true, gender = Gender.MALE)
    }
}
