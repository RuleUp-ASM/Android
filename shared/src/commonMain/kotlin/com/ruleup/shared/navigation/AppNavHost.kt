package com.ruleup.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 내비게이션 신호를 수신해 백스택을 갱신하고, 플랫폼별 [PlatformNavDisplay] 로 현재 스택을 렌더한다.
 * 백스택 자료구조(navigation3-runtime)는 멀티플랫폼이지만, NavDisplay 렌더링(navigation3-ui)은 iOS 를
 * 지원하지 않으므로 화면 렌더는 expect/actual 로 분리한다.
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val navigationHelper = LocalNavigationHelper.current

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> handleNavRoute(signal.route, backStack)
                NavSignal.Back -> backStack.removeLastOrNull()
            }
        }
    }

    PlatformNavDisplay(backStack = backStack, modifier = modifier)
}

private const val TAG = "[Navigation]"

/**
 * NavRoute 한 건을 받아 백스택에 push.
 * [AppRoute.isRoot] 페이지는 기존 스택을 모두 비우고 단일 키로 시작한다(가입 완료 → 홈 등).
 * 미등록 path 는 무시 + 경고 로그.
 */
fun handleNavRoute(
    route: NavRoute,
    backStack: NavBackStack<NavKey>,
) {
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        println("$TAG Unhandled NavRoute: ${route.path}")
        return
    }
    val navKey = GenericNavKey.of(route)

    if (appRoute.isRoot) {
        backStack.clear()
        backStack.add(navKey)
        return
    }

    if (backStack.lastOrNull() != navKey) {
        backStack.add(navKey)
    }
}
