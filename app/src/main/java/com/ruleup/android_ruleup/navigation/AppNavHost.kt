package com.ruleup.android_ruleup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.LocalScreenTracker
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.LocalObservability

/** 앱 백스택 관리 및 화면 표시. */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val navigationHelper = LocalNavigationHelper.current
    val screenTracker = LocalScreenTracker.current

    val observability = LocalObservability.current

    TrackVisibleScreen(backStack, screenTracker::onScreenEntered)

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> {
                    handleNavRoute(signal.route, backStack, observability)
                }

                is NavSignal.ReplaceStack -> {
                    replaceStack(signal.route, backStack, observability)
                }

                NavSignal.Back -> {
                    backStack.removeLastOrNull()
                }
            }
        }
    }

    PlatformNavDisplay(backStack = backStack, modifier = modifier)
}

private const val TAG = "[Navigation]"

/** 표시 중인 화면 진입 기록. */
@Composable
internal fun TrackVisibleScreen(
    backStack: NavBackStack<NavKey>,
    onEnter: (String) -> Unit,
) {
    val currentOnEntered = androidx.compose.runtime.rememberUpdatedState(onEnter)
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() as? GenericNavKey }.collect { key ->
            if (key != null && key.path in appRouteByPath) currentOnEntered.value(key.path)
        }
    }
}

fun handleNavRoute(
    route: NavRoute,
    backStack: NavBackStack<NavKey>,
    observability: Observability,
) {
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        // 등록되지 않은 path 로 이동 요청이 왔다
        observability.w(TAG) { "등록되지 않은 NavRoute 무시: ${route.path}" }
        return
    }
    val navKey = GenericNavKey.of(route)

    if (appRoute.isRoot) {
        // 이미 그 루트 단독이면 그대로 둔다
        if (backStack.size == 1 && backStack.first() == navKey) return
        backStack.clear()
        backStack.add(navKey)
        return
    }

    // 탭끼리 옮겨 다닌 기록을 쌓으면 뒤로가기가 직전 탭으로 간다
    if (appRoute.isBottomTab) {
        backStack.clear()
        backStack.addAll(appRoute.syntheticStack(route.args))
        return
    }

    if (backStack.lastOrNull() != navKey) {
        backStack.add(navKey)
    }
}

/** 백스택을 [route] 의 시작 스택으로 통째로 교체한다. */
fun replaceStack(
    route: NavRoute,
    backStack: NavBackStack<NavKey>,
    observability: Observability,
): Boolean {
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        observability.w(TAG) { "등록되지 않은 스택 교체 요청 무시: ${route.path}" }
        return false
    }
    backStack.clear()
    backStack.addAll(appRoute.syntheticStack(route.args))
    return true
}
