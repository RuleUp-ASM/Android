package com.ruleup.android_ruleup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val screenTracker = LocalScreenTracker.current

    val observability = LocalObservability.current

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> {
                    handleNavRoute(signal.route, backStack, observability)
                    screenTracker.onScreenEntered(signal.route.path)
                }

                is NavSignal.ReplaceStack -> {
                    if (replaceStack(signal.route, backStack, observability)) {
                        screenTracker.onScreenEntered(signal.route.path)
                    }
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

fun handleNavRoute(
    route: NavRoute,
    backStack: NavBackStack<NavKey>,
    observability: Observability,
) {
    val appRoute = appRouteByPath[route.path]
    if (appRoute == null) {
        // 등록되지 않은 path 로 이동 요청이 왔다 — 화면이 열리지 않고 조용히 끝나는 경로다.
        observability.w(TAG) { "등록되지 않은 NavRoute 무시: ${route.path}" }
        return
    }
    val navKey = GenericNavKey.of(route)

    if (appRoute.isRoot) {
        // 이미 그 루트 단독이면 그대로 둔다 — 다시 세우면 화면이 재생성돼 스플래시의 자동 로그인
        // 판정이 두 번 겹친다(자동 로그인 실패 → 세션 종료 감시가 스플래시를 다시 요청하는 경로).
        if (backStack.size == 1 && backStack.first() == navKey) return
        backStack.clear()
        backStack.add(navKey)
        return
    }

    if (backStack.lastOrNull() != navKey) {
        backStack.add(navKey)
    }
}

/**
 * 백스택을 [route] 의 시작 스택으로 통째로 교체한다. 교체했으면 true.
 *
 * 딥링크 목적지는 부모 화면이 함께 깔려야 뒤로가기가 자연스럽다(공지 상세 → 방 홈 → 홈).
 * 그래서 단일 키가 아니라 [AppRoute.syntheticStack] 을 쓴다.
 */
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
