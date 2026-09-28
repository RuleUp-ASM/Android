package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.ReportReason

/** 신고 사유 선택. */
@Composable
internal fun ReportReasonSheet(
    title: String,
    description: String,
    reasons: List<ReportReason>,
    selected: ReportReason?,
    submitting: Boolean,
    onSelect: (ReportReason) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScaffold(onDismiss = onDismiss) {
        val colors = RuleUpTheme.colors
        Text(text = title, color = colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
        Text(text = description, color = colors.textSecondary, style = RuleUpTheme.typography.body)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reasons.forEach { reason ->
                ReasonRow(
                    label = reason.label(),
                    selected = reason == selected,
                    onClick = { onSelect(reason) },
                )
            }
        }

        // 접수 후 아무 소식이 없는 게 정상이라는 걸 미리 알린다
        Text(
            text = "처리 결과는 따로 알려드리지 않아요 · 사유는 검토 참고용이에요",
            color = colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )

        RuleUpPrimaryButton(
            text = if (submitting) "접수 중" else "신고하기",
            onClick = onSubmit,
            enabled = selected != null && !submitting,
            modifier = Modifier.fillMaxWidth(),
        )
        CloseRow(onDismiss)
    }
}

/** 신고 완료. */
@Composable
internal fun ReportDoneSheet(
    effectMessage: String,
    onDismiss: () -> Unit,
) {
    SheetScaffold(onDismiss = onDismiss) {
        val colors = RuleUpTheme.colors
        Text(text = "신고를 접수했어요", color = colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
        Text(text = effectMessage, color = colors.textSecondary, style = RuleUpTheme.typography.body)
        Text(
            text = "차단은 내 화면에만 적용돼요 · 마이에서 풀 수 있어요",
            color = colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
        RuleUpPrimaryButton(text = "확인", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

/** 그래버 + 흰 카드. */
@Composable
private fun SheetScaffold(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RuleUpTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.medium)
                    .background(colors.surface)
                    .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RuleUpTheme.shapes.small)
                        .background(colors.border),
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ReasonRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Text(
        text = label,
        color = if (selected) colors.textPrimary else colors.textSecondary,
        style = RuleUpTheme.typography.bodyMedium,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(if (selected) colors.brandSoft else colors.surface)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) colors.brand else colors.border,
                    shape = RuleUpTheme.shapes.medium,
                ).singleClickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun CloseRow(onDismiss: () -> Unit) {
    Text(
        text = "닫기",
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.body,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .singleClickable(onClick = onDismiss)
                .padding(vertical = 10.dp),
    )
}

/** 사유의 화면 문구. */
internal fun ReportReason.label(): String =
    when (this) {
        ReportReason.CHEATING_SUSPECT -> "부정한 방법으로 인증한 것 같아요"
        ReportReason.INAPPROPRIATE -> "부적절한 내용이에요"
        ReportReason.SPAM_AD -> "광고 · 스팸이에요"
        ReportReason.ETC -> "그 외"
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ReportReasonSheetPreview() {
    RuleUpTheme {
        ReportReasonSheet(
            title = "신고하기",
            description = "신고 사유를 선택해 주세요",
            reasons = ReportReason.entries.toList(),
            selected = null,
            submitting = false,
            onSelect = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ReportDoneSheetPreview() {
    RuleUpTheme {
        ReportDoneSheet(effectMessage = "미리보기", onDismiss = { })
    }
}
