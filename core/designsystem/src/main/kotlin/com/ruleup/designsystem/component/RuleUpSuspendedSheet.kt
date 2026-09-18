package com.ruleup.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 기능 정지 안내 시트 (Figma `1465:141`).
 *
 * 정책이 **버튼을 숨기지 말라고** 정했다(제재 정책 §5.1·§5.2) — 신고·작성 진입점을 그대로 두고
 * 눌렀을 때 이 시트를 띄운다. 숨기면 사용자가 제재 사실 자체를 모른 채 "앱이 고장났다"고 읽는다.
 *
 * 신고 기능 정지와 콘텐츠 수정 정지가 문구만 다르고 구조가 같아 한 컴포넌트로 둔다.
 *
 * @param until 자동 해제 시각 문구. 기간 제재만 자동 해제되므로 없으면 그 줄을 빼고 그린다.
 * @param onOpenHistory 제재 이력으로 보낸다 — 사유와 해제일의 원본은 그 화면이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleUpSuspendedSheet(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    until: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = RuleUpTheme.colors.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = RuleUpTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.numberS)
            Text(
                text = until?.let { "$description\n$it 에 자동으로 풀려요." } ?: description,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            RuleUpPrimaryButton(text = "확인", onClick = onConfirm)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .singleClickable(onClick = onOpenHistory),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "제재 이력 보기",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun RuleUpSuspendedSheetPreview() {
    RuleUpTheme {
        RuleUpSuspendedSheet(
            title = "지금은 신고할 수 없어요",
            description = "허위 신고 반복으로 신고 기능이 정지됐어요.",
            until = "2026. 10. 15 00:00",
            onConfirm = {},
            onOpenHistory = {},
            onDismiss = {},
        )
    }
}
