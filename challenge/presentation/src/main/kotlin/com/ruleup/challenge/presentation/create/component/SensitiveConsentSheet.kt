package com.ruleup.challenge.presentation.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.AgreementType

/** 위치·건강 인증을 처음 쓰는 방을 만들기 직전에 받는 개별 동의. 동의하지 않으면 만들지 않는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SensitiveConsentSheet(
    type: AgreementType,
    onAgree: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, body) =
        when (type) {
            AgreementType.HEALTH_INFO ->
                "건강정보 수집 · 이용에 동의해 주세요" to
                    "걸음·운동·수면 기록으로 인증하려면 건강정보를 수집해요. 동의는 마이 › 계정 · 약관에서 언제든 철회할 수 있어요."
            else ->
                "위치정보 수집 · 이용에 동의해 주세요" to
                    "등록한 장소에 머문 시간으로 인증하려면 위치정보를 수집해요. 동의는 마이 › 계정 · 약관에서 언제든 철회할 수 있어요."
        }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RuleUpTheme.colors.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
            Text(text = body, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
            RuleUpPrimaryButton(text = "동의하고 만들기", onClick = onAgree, modifier = Modifier.fillMaxWidth())
            Text(
                text = "다음에 할게요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().singleClickable(onClick = onDismiss).padding(vertical = 8.dp),
            )
        }
    }
}
