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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.TodayResult

/**
 * 판정 결과 모달 (구 노드 `971:23`·`971:117` — 최종 페이지에 프레임이 없어 디자인 시스템으로 구성).
 *
 * 서버가 `unacknowledgedResult` 로 "아직 안 본 판정"을 알려주면 상세 진입 직후 한 번 올린다. 밤새
 * 확정된 결과를 사용자가 알 수 있는 유일한 자리라, 성공은 연속 기록이 어떻게 바뀌었는지를, 실패는
 * **왜 실패했고 이의를 낼 수 있는지**를 말한다.
 *
 * 뒤로 가기·바깥 탭으로도 닫힌다. 확인을 강제해 봐야 `ack` 는 멱등이고, 못 닫는 모달이 주는 손해가
 * 판정 하나를 다시 보여주는 것보다 크다.
 */
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

/** 미확인 판정이 실패인가. 성공 값은 계약이 여러 이름을 쓸 수 있어 실패만 특정한다. */
internal fun TodayResult.isFailedResult(): Boolean = unacknowledged?.result == RESULT_FAILED

/**
 * 결과 아래 한 줄. 성공은 연속 기록이 어떻게 늘었는지, 실패는 사유다.
 *
 * 점수 증분은 적지 않는다 — 점수는 회원당 누적 모델이라 이 화면이 정확한 값을 알 수 없고,
 * 어림수를 적으면 틀린 숫자를 확정처럼 보여주게 된다.
 */
internal fun resultNote(
    today: TodayResult,
    failed: Boolean,
): String? =
    if (failed) {
        today.failureReason?.failureText()
    } else {
        today.streak?.let { "${it.before}일 → ${it.after}일 연속" }
    }

/** 이의 마감 안내. 카드의 버튼 문구와 같은 계산을 쓴다(경계 시각 -1일). */
internal fun TodayResult.appealHint(): String {
    val until = appeal?.eligibleUntil?.let { appealDeadlineLabel(it) }
    return if (until == null) "이의를 제기할 수 있어요" else "${until}까지 이의를 제기할 수 있어요"
}

// unacknowledgedResult.result 의 실패 값. 성공 값은 여러 이름을 쓸 수 있어 실패만 특정한다.
internal const val RESULT_FAILED = "FAILED"
