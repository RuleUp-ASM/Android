package com.ruleup.verification.presentation.render

import androidx.compose.runtime.Composable

/**
 * 앱 권한 설정 화면을 여는 액션(명세 §6.4 권한 CTA). android = 앱 상세 설정, iOS = no-op.
 */
@Composable
expect fun rememberAppSettingsOpener(): () -> Unit
