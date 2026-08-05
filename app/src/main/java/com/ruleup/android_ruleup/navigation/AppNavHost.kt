package com.ruleup.android_ruleup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.LocalScreenTracker
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.AppEntry
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
    appEntryViewModel: AppEntryViewModel = hiltViewModel(),
) {
    val navigationHelper = LocalNavigationHelper.current
    val screenTracker = LocalScreenTracker.current

    val observability = LocalObservability.current

    // 앱 진입 목적지. 계산은 SessionBootstrap 이 하고 백스택 구성은 여기서 한다.
    LaunchedEffect(Unit) {
        appEntryViewModel.entry.collect { entry ->
            val route =
                when (entry) {
                    // 딥링크는 부모 화면까지 깔아야 뒤로가기가 자연스럽다. 전진 이동으로 처리하면
                    // 스플래시가 스택에 남아 뒤로 갔을 때 판정이 다시 돈다.
                    is AppEntry.DeepLink -> entry.route.also { replaceStack(it, backStack, observability) }
                    // 홈·로그인은 루트라 handleNavRoute 가 스택을 비우고 단독으로 세운다.
                    is AppEntry.Start -> entry.route.also { handleNavRoute(it, backStack, observability) }
                }
            screenTracker.onScreenEntered(route.path)
        }
    }

    LaunchedEffect(Unit) {
        navigationHelper.navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> {
                    handleNavRoute(signal.route, backStack, observability)
                    // 목적지 페이지 이동마다 관측 컨텍스트를 갱신하고 화면 진입을 기록한다.
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

/**
 * NavRoute 한 건을 받아 백스택에 push.
 * [AppRoute.isRoot] 페이지는 기존 스택을 모두 비우고 단일 키로 시작한다(가입 완료 → 홈 등).
 * 미등록 path 는 무시 + 경고 로그.
 */
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
