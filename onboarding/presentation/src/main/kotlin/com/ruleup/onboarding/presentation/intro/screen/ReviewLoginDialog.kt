package com.ruleup.onboarding.presentation.intro.screen

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.ruleup.designsystem.theme.RuleUpTheme

@Composable
internal fun ReviewLoginDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isSubmitting: Boolean = false,
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("심사 계정 로그인") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("심사 코드") },
                singleLine = true,
                enabled = !isSubmitting,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(code) }, enabled = code.isNotBlank() && !isSubmitting) {
                Text("로그인", color = RuleUpTheme.colors.textPrimary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("취소") } },
        containerColor = RuleUpTheme.colors.surface,
    )
}
