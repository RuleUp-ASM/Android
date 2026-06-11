package com.ruleup.shared.navigation

import androidx.compose.runtime.Composable

/**
 * 짧은 토스트 메시지를 표시하는 플랫폼별 함수.
 * - Android: android.widget.Toast.
 * - iOS: (현재) 로그 출력. 추후 UIKit 토스트/배너로 대체 가능.
 */
@Composable
expect fun rememberShowToast(): (String) -> Unit
