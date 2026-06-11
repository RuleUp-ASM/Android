package com.ruleup.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 백스택을 화면으로 렌더하는 플랫폼별 디스플레이.
 * - Android: navigation3-ui 의 NavDisplay(전환/ViewModelStore 데코레이터 포함).
 * - iOS: navigation3-ui 가 iOS 를 미지원하므로, 최상단 엔트리를 직접 렌더하는 간단 디스플레이.
 */
@Composable
expect fun PlatformNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
)
