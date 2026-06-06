package com.ruleup.onboarding.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ruleup.entity.user.Agreement
import com.ruleup.onboarding.presentation.profile.component.InfoBox
import com.ruleup.onboarding.presentation.profile.component.ProfileFlowPreview
import com.ruleup.onboarding.presentation.profile.component.RequirementBadge
import com.ruleup.onboarding.presentation.profile.component.RowDivider
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/** 05 · 약관 동의 (5/5). 필수 약관(이용약관·개인정보)에 모두 동의해야 가입을 진행할 수 있다. */
@Composable
fun AgreementsContent(
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    agreements: Agreement = Agreement(terms = false, privacy = false, marketing = false),
) {
    val nav = LocalNavigationHelper.current
    ProfileSetupScaffold(
        step = 4,
        buttonText = "RuleUp 시작하기",
        modifier = modifier,
        onNext = { onIntent(ProfileIntent.Submit) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "약관에 동의해주세요",
            subtitle = "서비스 이용을 위해 동의가 필요해요",
            titleSize = 22,
        )

        val allChecked = agreements.terms && agreements.privacy && agreements.marketing

        // 전체 동의
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.brandSoft)
                    .clickable {
                        onIntent(
                            ProfileIntent.SetAgreements(
                                Agreement(
                                    terms = !allChecked,
                                    privacy = !allChecked,
                                    marketing = !allChecked,
                                ),
                            ),
                        )
                    }.padding(horizontal = 14.dp),
        ) {
            AgreementRow(
                checked = allChecked,
                label = "전체 동의",
                emphasize = true,
            )
        }

        // 개별 약관
        val items =
            listOf(
                AgreementItem(
                    "서비스 이용약관",
                    required = true,
                    checked = agreements.terms,
                ) { agreements.copy(terms = it) },
                AgreementItem(
                    "개인정보 처리방침",
                    required = true,
                    checked = agreements.privacy,
                ) { agreements.copy(privacy = it) },
                AgreementItem(
                    "마케팅 정보 수신",
                    required = false,
                    checked = agreements.marketing,
                ) { agreements.copy(marketing = it) },
            )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card),
        ) {
            items.forEachIndexed { index, item ->
                AgreementRow(
                    checked = item.checked,
                    label = item.label,
                    required = item.required,
                    modifier =
                        Modifier.clickable {
                            onIntent(ProfileIntent.SetAgreements(item.toggle(!item.checked)))
                        },
                )
                if (index != items.lastIndex) RowDivider()
            }
        }

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "📄",
            text = "필수 항목에 동의해야 가입을 완료할 수 있어요. 마케팅 수신은 선택이에요",
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

/** 개별 약관 한 줄을 그리기 위한 데이터. [toggle] 은 체크 상태를 바꾼 새 [Agreements] 를 만든다. */
private class AgreementItem(
    val label: String,
    val required: Boolean,
    val checked: Boolean,
    val toggle: (Boolean) -> Agreement,
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AgreementScreenPreview() {
    ProfileFlowPreview {
        AgreementsContent(onIntent = {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AgreementScreenDarkPreview() {
    ProfileFlowPreview(darkTheme = true) { AgreementsContent(onIntent = {}) }
}
