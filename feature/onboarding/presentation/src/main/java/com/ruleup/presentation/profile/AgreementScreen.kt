package com.ruleup.presentation.profile

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
import com.ruleup.core.designsystem.component.ProfileSetupScaffold
import com.ruleup.core.designsystem.theme.RuleUpGradients
import com.ruleup.core.designsystem.theme.RuleUpTheme
import com.ruleup.domain.auth.model.Agreements

/** 05 · 약관 동의 (5/5). 필수 약관(이용약관·개인정보)에 모두 동의해야 가입을 진행할 수 있다. */
@Composable
fun AgreementsContent(
    modifier: Modifier = Modifier,
    agreements: Agreements = Agreements(terms = false, privacy = false, marketing = false),
    onAgreementsChange: (Agreements) -> Unit = {},
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    ProfileSetupScaffold(
        step = 4,
        buttonText = "RuleUp 시작하기",
        modifier = modifier,
        onNext = onNext,
        onBack = onBack,
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
                    .clickable { onAgreementsChange(Agreements(terms = !allChecked, privacy = !allChecked, marketing = !allChecked)) }
                    .padding(horizontal = 14.dp),
        ) {
            AgreementRow(
                checked = allChecked,
                label = "전체 동의",
                emphasize = true,
            )
        }

        // 개별 약관
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card),
        ) {
            AgreementRow(
                checked = agreements.terms,
                label = "서비스 이용약관",
                required = true,
                modifier = Modifier.clickable { onAgreementsChange(agreements.copy(terms = !agreements.terms)) },
            )
            RowDivider()
            AgreementRow(
                checked = agreements.privacy,
                label = "개인정보 처리방침",
                required = true,
                modifier = Modifier.clickable { onAgreementsChange(agreements.copy(privacy = !agreements.privacy)) },
            )
            RowDivider()
            AgreementRow(
                checked = agreements.marketing,
                label = "마케팅 정보 수신",
                required = false,
                modifier = Modifier.clickable { onAgreementsChange(agreements.copy(marketing = !agreements.marketing)) },
            )
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

@Composable
private fun RequirementBadge(required: Boolean) {
    val background = if (required) RuleUpTheme.colors.danger else RuleUpTheme.colors.surfaceVariant
    val textColor = if (required) Color.White else RuleUpTheme.colors.textSecondary
    Box(
        modifier =
            Modifier
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(background)
                .padding(horizontal = RuleUpTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (required) "필수" else "선택",
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.36.sp,
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RuleUpTheme.colors.border),
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AgreementScreenPreview() {
    RuleUpTheme {
        AgreementsContent(agreements = Agreements(terms = true, privacy = true, marketing = false))
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AgreementScreenDarkPreview() {
    RuleUpTheme(darkTheme = true) { AgreementsContent() }
}
