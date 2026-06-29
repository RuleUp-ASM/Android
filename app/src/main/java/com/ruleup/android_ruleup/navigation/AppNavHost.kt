package com.ruleup.android_ruleup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ruleup.analytics.AnalyticsEvent
import com.ruleup.android_ruleup.LocalAnalyticsLogger
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 내비게이션 신호를 수신해 백스택(navigation3-runtime)을 갱신하고,
 * [PlatformNavDisplay](navigation3-ui)로 현재 스택을 렌더한다.
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val navigationHelper = LocalNavigationHelper.current
    val analyticsLogger = LocalAnalyticsLogger.current

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> {
                    handleNavRoute(signal.route, backStack)
                    // 목적지 페이지 이동마다 화면 진입을 기록한다(path 를 화면 이름으로 사용).
                    analyticsLogger.log(AnalyticsEvent.ScreenView(screen = signal.route.path))
                }
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
