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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.ReportReason

/**
 * 신고 사유 선택 (Figma `1285:2`).
 *
 * **자유 텍스트 입력이 없다.** 서버가 사유 선택만 받도록 바뀌었고(2026-08-26), 입력칸을 두면
 * 적어 봐야 어디에도 전달되지 않는 글을 사용자가 쓰게 된다.
 *
 * 고를 수 있는 사유 목록은 [reasons] 로 받는다 — 대상이 무엇인지 이 시트가 판단하지 않는다.
 * 챌린지에는 `CHEATING_SUSPECT` 가 없고, 그 제약은 `ReportTarget` 이 이미 갖고 있다.
 */
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

        // 접수 후 아무 소식이 없는 게 정상이라는 걸 미리 알린다 — 모르면 "무시당했다"고 읽는다.
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

/**
 * 신고 완료 (Figma `1286:2`).
 *
 * 세 문구는 서버가 내려주는 `hiddenEffect` 세 값에 그대로 대응한다 — 무엇이 가려졌는지 말해
 * 주지 않으면 사용자는 신고가 먹혔는지 알 수 없다.
 */
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

/** 그래버 + 흰 카드. 기존 시트(`AppealSheet`)와 같은 껍데기다. */
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

/**
 * 사유의 화면 문구. 서버 enum 이름을 그대로 보여주면 사용자가 읽을 수 없고, 문구를 domain 에
 * 두면 domain 이 화면 어휘를 갖게 된다 — 번역은 화면 몫이다.
 */
internal fun ReportReason.label(): String =
    when (this) {
        ReportReason.CHEATING_SUSPECT -> "부정한 방법으로 인증한 것 같아요"
        ReportReason.INAPPROPRIATE -> "부적절한 내용이에요"
        ReportReason.SPAM_AD -> "광고 · 스팸이에요"
        ReportReason.ETC -> "그 외"
    }
