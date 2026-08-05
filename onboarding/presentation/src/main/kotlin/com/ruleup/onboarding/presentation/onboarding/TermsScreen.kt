package com.ruleup.onboarding.presentation.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.onboarding.domain.observability.OnboardingStep
import com.ruleup.onboarding.presentation.component.OnboardingScaffold
import com.ruleup.onboarding.presentation.onboarding.component.InfoBox
import com.ruleup.onboarding.presentation.onboarding.component.OnboardingFlowPreview
import com.ruleup.onboarding.presentation.onboarding.component.RequirementBadge
import com.ruleup.onboarding.presentation.onboarding.component.RowDivider
import com.ruleup.onboarding.presentation.onboarding.component.SectionHeader
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 06 · 약관 동의 (6/6). 필수 3종(이용약관·개인정보·위치기반)에 모두 동의해야 가입할 수 있다.
 *
 * [checked] 에 없는 항목도 전송 시 `agreed=false` 로 기록된다 — 선택 약관의 "동의 안 함"까지
 * 남겨야 약관이 개정됐을 때 재동의 판정을 할 수 있다.
 */
@Composable
fun TermsContent(
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
    checked: Set<AgreementType> = emptySet(),
    submitting: Boolean = false,
) {
    val nav = LocalNavigationHelper.current
    OnboardingScaffold(
        step = OnboardingStep.TERMS,
        buttonText = "시작하기",
        modifier = modifier,
        // 필수 3종을 다 채우기 전엔 눌러도 서버가 REQUIRED_AGREEMENT_MISSING 으로 튕긴다.
        nextEnabled = AgreementType.REQUIRED.all { it in checked } && !submitting,
        onNext = { onIntent(OnboardingIntent.Submit) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "마지막이에요",
            subtitle = "서비스 이용을 위해 동의가 필요해요",
            titleSize = 22,
        )

        val allChecked = checked.containsAll(AgreementType.entries)

        // 전체 동의
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.brandSoft)
                    .singleClickable(globalGuard = false) {
                        onIntent(OnboardingIntent.ToggleAllAgreements)
                    }.padding(horizontal = 14.dp),
        ) {
            AgreementRow(
                checked = allChecked,
                label = "전체 동의",
                emphasize = true,
            )
        }

        // 개별 약관 6종
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card),
        ) {
            AgreementType.entries.forEachIndexed { index, type ->
                AgreementRow(
                    checked = type in checked,
                    label = type.label(),
                    required = type.required,
                    modifier =
                        Modifier.singleClickable(globalGuard = false) {
                            onIntent(OnboardingIntent.ToggleAgreement(type))
                        },
                )
                if (index != AgreementType.entries.lastIndex) RowDivider()
            }
        }

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "📄",
            text = "필수 항목에 동의해야 가입을 완료할 수 있어요. 선택 항목은 알림 설정 기본값이 돼요",
            textColor = RuleUpTheme.colors.brandStrong,
        )
    }
}

/** 동의 한 줄: 체크 원 + 라벨 + (필수/선택) 배지. [required] 가 null 이면 배지를 숨긴다(전체 동의). */
@Composable
private fun AgreementRow(
    checked: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean? = null,
    emphasize: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked = checked)
        Text(
            label,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = if (emphasize) 15.sp else 14.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
        )
        if (required != null) {
            RequirementBadge(required = required)
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean) {
    val base =
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(11.dp))
    Box(
        modifier =
            if (checked) {
                base.background(RuleUpGradients.Button)
            } else {
                base
                    .background(RuleUpTheme.colors.surfaceVariant)
                    .border(1.dp, RuleUpTheme.colors.borderStrong, RoundedCornerShape(11.dp))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 약관 표시명. 서버 키(`AgreementType.key`)와 달리 화면 문구라 여기서 정한다. */
private fun AgreementType.label(): String =
    when (this) {
        AgreementType.TERMS_OF_SERVICE -> "서비스 이용약관"
        AgreementType.PRIVACY_POLICY -> "개인정보 수집·이용"
        AgreementType.LOCATION_SERVICE -> "위치·센서 정보 활용 (자동 인증)"
        AgreementType.MARKETING -> "마케팅 정보 수신"
        AgreementType.EVENT -> "이벤트 정보 수신"
        AgreementType.NIGHT_PUSH -> "야간 푸시 알림 (21~08시)"
    }

@Preview
@Composable
private fun TermsScreenPreview() {
    OnboardingFlowPreview {
        TermsContent(onIntent = {})
    }
}

@Preview
@Composable
private fun AgreementScreenDarkPreview() {
    OnboardingFlowPreview(darkTheme = true) { TermsContent(onIntent = {}) }
}
