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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * [AuthFailureUi] 를 실제 화면으로 그린다.
 *
 * 무게별 표현을 한곳에 모은다 — 화면마다 다이얼로그를 새로 만들면 같은 실패가 어디서는 토스트,
 * 어디서는 전체 화면으로 갈린다.
 *
 * 토스트는 여기서 그리지 않는다. 컴포지션 밖(MessageHelper)에서 띄우는 게 자연스러워 호출부가
 * 처리하고, 이 컴포저블은 화면을 덮는 두 종류만 맡는다.
 */
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
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
