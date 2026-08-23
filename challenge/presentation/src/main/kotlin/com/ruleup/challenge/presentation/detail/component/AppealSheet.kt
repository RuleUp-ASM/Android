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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.AppealPolicy
import com.ruleup.verification.domain.entity.TodayResult

/**
 * 이의 작성 (Figma `1134:768`).
 *
 * **판정을 기다리는 신청서가 아니다** — 형식 요건만 맞으면 바로 인용된다. 그래서 "검토해 드릴게요"·
 * "심사 중" 같은 표현을 쓰지 않는다.
 *
 * 상단에 **비공개 고지**를 먼저 둔다. 지금 이의하면 실패가 그룹에 공개되지 않는다는 것이 사용자가
 * 이 화면에서 얻는 실익이고, 그걸 모르면 "귀찮은 이의 절차"로만 보인다.
 *
 * Figma 헤더 우측의 "2/3 남음"은 따르지 않는다 — 이의 횟수 한도가 폐기됐다(챌린지 정책 §7.2).
 * 그 자리에는 명세가 지정한 **신청 마감 시각**을 둔다.
 */
@Composable
internal fun AppealSheet(
    today: TodayResult,
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
                today.appealDeadlineText()?.let {
                    Text(text = it, color = colors.textMuted, style = RuleUpTheme.typography.caption)
                }
            }

            // 이의의 실익을 먼저 말한다 — 지금 내면 실패가 그룹에 공개되지 않는다.
            Text(
                text = today.privacyNotice(),
                color = colors.brand,
                style = RuleUpTheme.typography.caption,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.medium)
                        .background(colors.brandSoft)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
            )

            // 무엇에 대한 이의인지 — 대상 인증 건과 실패 사유.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.medium)
                        .border(1.dp, colors.border, RuleUpTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(text = today.date, color = colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
                today.failureReason?.let {
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
            // 글자수와 제출 가능 여부를 입력 중에 알린다 — 다 쓰고 나서 400 을 보는 왕복을 없앤다.
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

/** 입력 중 상태 문구. 하한을 넘긴 순간 "제출할 수 있어요"로 바뀌어 다음 행동을 알린다. */
internal fun reasonCounter(
    length: Int,
    enough: Boolean,
): String = if (enough) "${length}자 · 제출할 수 있어요" else "${AppealPolicy.MIN_REASON_LENGTH}자 이상 적어 주세요"

/** 헤더 우측 마감 안내. 카드·모달과 같은 계산을 쓴다(경계 시각 −1일). */
internal fun TodayResult.appealDeadlineText(): String? = appeal?.eligibleUntil?.let { appealDeadlineLabel(it) }?.let { "${it}까지" }

/**
 * 비공개 고지. 아직 공개되지 않았다는 사실과 "지금 내면 공개되지 않는다"를 함께 말한다 —
 * 둘 중 하나만 있으면 실익이 드러나지 않는다.
 */
internal fun TodayResult.privacyNotice(): String {
    val until = appeal?.eligibleUntil?.let { appealDeadlineLabel(it) }
    return if (until == null) {
        "아직 그룹에 공개되지 않았어요 — 지금 이의하면 공개되지 않아요"
    } else {
        "아직 그룹에 공개되지 않았어요 — ${until}까지 이의하면 공개되지 않아요"
    }
}
