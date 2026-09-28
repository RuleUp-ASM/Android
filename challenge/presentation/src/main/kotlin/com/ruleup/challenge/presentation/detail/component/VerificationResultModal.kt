package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.failureText

/** 판정 결과 모달. */
@Composable
internal fun VerificationResultModal(
    today: TodayResult,
    onConfirm: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    val failed = today.isFailedResult()
    Dialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Column(
            modifier =
                Modifier
                    .clip(RuleUpTheme.shapes.card)
                    .background(colors.surface)
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (failed) colors.danger else colors.success),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (failed) "!" else "✓",
                    color = colors.surface,
                    style = RuleUpTheme.typography.title,
                )
            }
            Text(
                text = if (failed) "오늘 인증을 놓쳤어요" else "오늘 인증 성공!",
                color = colors.textPrimary,
                style = RuleUpTheme.typography.section,
                textAlign = TextAlign.Center,
            )
            resultNote(today, failed)?.let {
                Text(
                    text = it,
                    color = colors.textSecondary,
                    style = RuleUpTheme.typography.body,
                    textAlign = TextAlign.Center,
                )
            }
            if (failed && today.appeal?.eligible == true) {
                Text(
                    text = today.appealHint(),
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                    textAlign = TextAlign.Center,
                )
            }
            RuleUpPrimaryButton(
                text = "확인",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 미확인 판정이 실패인가. */
internal fun TodayResult.isFailedResult(): Boolean = unacknowledged?.result == RESULT_FAILED

/** 결과 아래 한 줄. */
internal fun resultNote(
    today: TodayResult,
    failed: Boolean,
): String? =
    if (failed) {
        today.failureReason?.failureText()
    } else {
        today.streak?.let { "${it.before}일 → ${it.after}일 연속" }
    }

/** 이의 마감 안내. */
internal fun TodayResult.appealHint(): String {
    val until = appeal?.eligibleUntil?.let { appealDeadlineLabel(it) }
    return if (until == null) "이의를 제기할 수 있어요" else "${until}까지 이의를 제기할 수 있어요"
}

// unacknowledgedResult.result 의 실패 값.
internal const val RESULT_FAILED = "FAILED"

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun VerificationResultModalPreview() {
    RuleUpTheme {
        VerificationResultModal(
            today =
                com.ruleup.verification.domain.entity.TodayResult(
                    date = "2026-09-28",
                    verificationId = null,
                    status = null,
                    window = null,
                    confirmedAt = null,
                    failureReason = null,
                    streak = null,
                    unacknowledged = null,
                    appeal = null,
                ),
            onConfirm = {
            },
        )
    }
}
