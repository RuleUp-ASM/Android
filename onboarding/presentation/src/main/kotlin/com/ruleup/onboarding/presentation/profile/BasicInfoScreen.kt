package com.ruleup.onboarding.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.ruleup.onboarding.domain.navigation.ProfileAgreementPage
import com.ruleup.onboarding.presentation.profile.component.InfoBox
import com.ruleup.onboarding.presentation.profile.component.ProfileFlowPreview
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.onboarding.presentation.profile.viewmodel.OnboardingGender
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/**
 * 05 · 나이·성별 (5/6). 맞춤 추천용 선택 입력이라 "건너뛰기"로 넘어갈 수 있고,
 * 입력값은 가입 완료 후 PUT /onboarding/me 로 전송된다(ViewModel 이 처리).
 */
@Composable
fun BasicInfoContent(
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    age: Int? = null,
    gender: OnboardingGender? = null,
    genderDeclined: Boolean = false,
) {
    val nav = LocalNavigationHelper.current
    ProfileSetupScaffold(
        step = 4,
        buttonText = "다음",
        modifier = modifier,
        onNext = { nav.navigateTo(ProfileAgreementPage) },
        onBack = { nav.navigateToBack() },
        onSkip = { nav.navigateTo(ProfileAgreementPage) },
    ) {
        SectionHeader(
            title = "나이와 성별을 알려주세요",
            subtitle = "비슷한 또래의 챌린지를 추천해드려요",
            titleSize = 22,
        )

        AgeSection(age = age, onAgeChange = { onIntent(ProfileIntent.SetAge(it)) })

        GenderSection(
            gender = gender,
            declined = genderDeclined,
            onSelect = { onIntent(ProfileIntent.SetGender(it)) },
            onDecline = { onIntent(ProfileIntent.DeclineGender) },
        )

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "ℹ️",
            text = "나이·성별은 맞춤 추천에만 사용되고 다른 사용자에게 공개되지 않아요",
            textColor = RuleUpTheme.colors.brandStrong,
        )
    }
}

/** 나이 입력: 라벨("만 나이 기준" 힌트) + 숫자 입력 박스("세" 접미). */
@Composable
private fun AgeSection(
    age: Int?,
    onAgeChange: (Int?) -> Unit,
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
            Text("나이", color = RuleUpTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("만 나이 기준", color = RuleUpTheme.colors.textSecondary, fontSize = 12.sp)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = age?.toString().orEmpty(),
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(3)
                    onAgeChange(digits.toIntOrNull())
                },
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
                    if (age == null) {
                        Text("나이 입력", color = RuleUpTheme.colors.textSecondary, fontSize = 16.sp)
                    }
                    inner()
                },
            )
            Text("세", color = RuleUpTheme.colors.textSecondary, fontSize = 16.sp)
        }
    }
}

/** 성별 선택: 남성/여성 카드 + "응답하지 않을래요" 라디오. */
@Composable
private fun GenderSection(
    gender: OnboardingGender?,
    declined: Boolean,
    onSelect: (OnboardingGender) -> Unit,
    onDecline: () -> Unit,
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
                selected = gender == OnboardingGender.MALE,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(OnboardingGender.MALE) },
            )
            GenderCard(
                glyph = "♀",
                label = "여성",
                selected = gender == OnboardingGender.FEMALE,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(OnboardingGender.FEMALE) },
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .singleClickable(globalGuard = false) { onDecline() },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioDot(selected = declined)
            Text(
                "응답하지 않을래요",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 14.sp,
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

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier =
            Modifier
                .size(16.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(RuleUpGradients.Button)
                    } else {
                        Modifier.border(1.dp, RuleUpTheme.colors.borderStrong, CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Preview
@Composable
private fun BasicInfoScreenPreview() {
    ProfileFlowPreview {
        BasicInfoContent(onIntent = {}, age = 28, gender = OnboardingGender.MALE)
    }
}
