package com.ruleup.report.presentation.blocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.ruleup.report.presentation.blocklist.viewmodel.BlockTarget

/**
 * 차단 해제 확인 (Figma `1287:62`).
 *
 * 본문의 "신고 기록은 그대로 남아요"가 이 시트의 핵심이다 — 해제를 신고 취소로 오해하면,
 * 가해자가 피해자에게 해제를 종용해 기록을 지우게 만드는 경로가 생긴다. 문구를 줄이지 않는다.
 */
@Composable
internal fun UnblockConfirmSheet(
    target: BlockTarget,
    submitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
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
            Box(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
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
            ) {
                Text(
                    text = "차단을 풀까요?",
                    color = colors.textPrimary,
                    style = RuleUpTheme.typography.cardTitle,
                )
                Text(
                    text = target.confirmBody(),
                    color = colors.textSecondary,
                    style = RuleUpTheme.typography.body,
                )
                RuleUpPrimaryButton(
                    text = if (submitting) "푸는 중" else "차단 해제",
                    onClick = onConfirm,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "닫기",
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.body,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .singleClickable(onClick = onDismiss)
                            .padding(vertical = 10.dp),
                )
            }
        }
    }
}

/** 사용자와 챌린지는 풀었을 때 다시 보이는 것이 달라 문장을 나눈다. */
internal fun BlockTarget.confirmBody(): String =
    when (this) {
        is BlockTarget.User -> "이 사람의 글과 프로필이 다시 보여요. 신고 기록은 그대로 남아요."
        is BlockTarget.Challenge -> "이 챌린지가 탐색 목록에 다시 나타나요. 신고 기록은 그대로 남아요."
    }
