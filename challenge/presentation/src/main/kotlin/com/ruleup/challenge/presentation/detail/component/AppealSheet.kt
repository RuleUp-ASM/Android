package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.AppealPolicy
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.failureText

/** 이의 대상 한 건. */
internal data class AppealTarget(
    val verificationId: String,
    val date: String,
    val failureReason: FailureReason?,
    val eligibleUntil: String?,
)

/** 오늘 결과를 이의 대상으로. */
internal fun TodayResult.toAppealTarget(): AppealTarget? =
    verificationId?.let {
        AppealTarget(
            verificationId = it,
            date = date,
            failureReason = failureReason,
            eligibleUntil = appeal?.eligibleUntil,
        )
    }

/** 이의 작성. */

@Composable
internal fun AppealSheet(
    target: AppealTarget,
    submitting: Boolean,
    imageUrl: String?,
    uploadingImage: Boolean,
    reasonError: String?,
    onPickImage: () -> Unit,
    onSubmit: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    val trimmed = reason.trim()
    val enough = trimmed.length >= AppealPolicy.MIN_REASON_LENGTH
    val colors = RuleUpTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Column(
            modifier =
                Modifier
                    .clip(RuleUpTheme.shapes.card)
                    .background(colors.surface)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "이의 제기",
                    color = colors.textPrimary,
                    style = RuleUpTheme.typography.section,
                )
                Box(Modifier.weight(1f))
                target.appealDeadlineText()?.let {
                    Text(text = it, color = colors.textMuted, style = RuleUpTheme.typography.caption)
                }
            }

            // 이의의 실익을 먼저 말한다
            Text(
                text = target.privacyNotice(),
                color = colors.brand,
                style = RuleUpTheme.typography.caption,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.medium)
                        .background(colors.brandSoft)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
            )

            // 무엇에 대한 이의인지
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.medium)
                        .border(1.dp, colors.border, RuleUpTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(text = target.date, color = colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
                target.failureReason?.let {
                    Text(text = it.failureText(), color = colors.textMuted, style = RuleUpTheme.typography.caption)
                }
            }

            Text(
                text = "무슨 일이 있었나요?",
                color = colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("예: 지하철 구간에서 GPS 가 끊겨 체류 기록이 빠졌어요") },
                minLines = 3,
                isError = reasonError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            // 글자수와 제출 가능 여부를 입력 중에 알린다
            Text(
                text = reasonError ?: reasonCounter(trimmed.length, enough),
                color =
                    when {
                        reasonError != null -> colors.danger
                        enough -> colors.success
                        else -> colors.textMuted
                    },
                style = RuleUpTheme.typography.caption,
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "사진", color = colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
                Text(
                    text = " (선택)",
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(RuleUpTheme.shapes.medium)
                        .border(1.dp, colors.border, RuleUpTheme.shapes.medium)
                        .singleClickable(enabled = !uploadingImage, onClick = onPickImage),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        when {
                            uploadingImage -> "올리는 중"
                            imageUrl != null -> "첨부됨"
                            else -> "추가"
                        },
                    color = if (imageUrl != null) colors.brand else colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }

            RuleUpPrimaryButton(
                text = if (submitting) "보내는 중…" else "제출하기",
                onClick = { onSubmit(trimmed) },
                enabled = !submitting && enough,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "취소",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .singleClickable(onClick = onDismiss)
                        .padding(vertical = 8.dp),
            )
        }
    }
}

/** 입력 중 상태 문구. */
internal fun reasonCounter(
    length: Int,
    enough: Boolean,
): String = if (enough) "${length}자 · 제출할 수 있어요" else "${AppealPolicy.MIN_REASON_LENGTH}자 이상 적어 주세요"

/** 헤더 우측 마감 안내. */
internal fun AppealTarget.appealDeadlineText(): String? = eligibleUntil?.let { appealDeadlineLabel(it) }?.let { "${it}까지" }

/** 비공개 고지. */
internal fun AppealTarget.privacyNotice(): String {
    val until = eligibleUntil?.let { appealDeadlineLabel(it) }
    return if (until == null) {
        "아직 그룹에 공개되지 않았어요 — 지금 이의하면 공개되지 않아요"
    } else {
        "아직 그룹에 공개되지 않았어요 — ${until}까지 이의하면 공개되지 않아요"
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AppealSheetPreview() {
    RuleUpTheme {
        AppealSheet(
            target =
                com.ruleup.challenge.presentation.detail.component.AppealTarget(
                    verificationId = "미리보기",
                    date = "2026-09-28",
                    failureReason = null,
                    eligibleUntil = null,
                ),
            submitting = false,
            imageUrl = null,
            uploadingImage = false,
            reasonError = null,
            onPickImage = {
            },
            onSubmit = { },
            onDismiss = { },
        )
    }
}
