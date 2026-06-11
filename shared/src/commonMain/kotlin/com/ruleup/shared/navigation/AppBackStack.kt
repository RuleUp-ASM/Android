package com.ruleup.shared.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 시작 스택으로 초기화된 [NavBackStack] 을 생성한다.
 * navigation3-runtime 의 rememberNavBackStack 시그니처가 Android·iOS 아티팩트에서 달라
 * 공통 호출이 불가하므로 플랫폼별 actual 로 분리한다.
 */
@Composable
expect fun rememberAppBackStack(startStack: List<NavKey>): NavBackStack<NavKey>
