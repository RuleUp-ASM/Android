package com.ruleup.onboarding.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme

/** [AuthFailureUi] 를 실제 화면으로 그린다. */
@Composable
fun AuthFailureHost(
    ui: AuthFailureUi?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onContactSupport: () -> Unit = {},
) {
    when (ui) {
        null, is AuthFailureUi.Toast -> Unit

        is AuthFailureUi.Dialog ->
            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(if (ui.restartFromLogin) "처음부터" else "확인")
                    }
                },
                text = { Text(ui.message) },
                containerColor = RuleUpTheme.colors.surface,
            )

        is AuthFailureUi.Blocking ->
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .background(RuleUpTheme.colors.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = ui.message,
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.section,
                    )
                    if (ui.contactSupport) {
                        TextButton(onClick = onContactSupport) {
                            Text("문의하기", color = RuleUpTheme.colors.brand)
                        }
                    }
                }
            }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390)
@Composable
private fun AuthFailureHostPreview() {
    RuleUpTheme {
        AuthFailureHost(ui = AuthFailureUi.Dialog("잠시 후 다시 시도해 주세요"), onDismiss = {})
    }
}
