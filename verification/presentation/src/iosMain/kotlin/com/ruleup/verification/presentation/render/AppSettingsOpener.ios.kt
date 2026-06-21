package com.ruleup.verification.presentation.render

import androidx.compose.runtime.Composable

// iOS 자동인증은 GPS 단독으로 축소(명세 §0·§8) — 권한 설정 딥링크는 후속. 컴파일 대칭만 맞춘다.
@Composable
actual fun rememberAppSettingsOpener(): () -> Unit = {}
