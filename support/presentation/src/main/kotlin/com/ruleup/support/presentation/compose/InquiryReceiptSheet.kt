package com.ruleup.support.presentation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.support.presentation.common.shortInquiryId

/**
 * 문의 접수 완료 (Figma `1419:2`).
 *
 * 접수번호를 여기서만 처음 보여 준다. **복사는 줄이지 않은 원문을 넘긴다** — 화면에 줄여 쓴 값을
 * 붙여넣으면 CS 담당자가 조회하지 못한다.
 *
 * Figma 와 다르게 간 곳
 * - **"답변이 오면 알림함으로 알려드려요"를 뺐다.** 답변을 알림함으로 알리지 않기로 했다
 *   (2026-09-11). 대신 어디서 확인하는지를 말한다 — 알려 주지 않으면 사용자는 답변을 기다리다
 *   같은 문의를 다시 넣고 하루 상한만 깎인다.
 */
@Composable
internal fun InquiryReceiptSheet(
    inquiryId: String,
    onConfirm: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    val clipboard = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onConfirm,
        // 접수번호를 보기 전에 닫히면 다시 볼 길이 내역 화면뿐이라, 바깥 탭으로는 닫지 않는다.
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.medium)
                    .background(colors.surface)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RuleUpTheme.shapes.large).background(colors.successContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✓", color = colors.success, style = RuleUpTheme.typography.title)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "문의가 접수됐어요",
                color = colors.textPrimary,
                style = RuleUpTheme.typography.title,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "평일 10:00~18:00 운영 · 영업일 2일 안에 답변드려요",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.small)
                        .background(colors.surfaceVariant)
                        .singleClickable { clipboard.setText(AnnotatedString(inquiryId)) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "접수번호",
                    color = colors.textSecondary,
                    style = RuleUpTheme.typography.caption,
                )
                Text(
                    text = shortInquiryId(inquiryId),
                    color = colors.textPrimary,
                    style = RuleUpTheme.typography.smallMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "답변은 설정의 「내 문의 내역」에서 확인할 수 있어요",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
            RuleUpPrimaryButton(text = "확인", onClick = onConfirm)
        }
    }
}
