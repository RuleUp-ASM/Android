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

/** 기능 정지 안내 시트. */
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
