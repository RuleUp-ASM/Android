package com.ruleup.shared.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * iOS 디스플레이. navigation3-ui(NavDisplay)가 iOS 를 미지원하므로 백스택 최상단 엔트리를 직접 렌더한다.
 * 전환 애니메이션·per-entry ViewModelStore 데코레이터는 아직 없다(추후 개선 여지).
 */
@Composable
actual fun PlatformNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier,
) {
    val current = backStack.lastOrNull() as? GenericNavKey
    Box(modifier.fillMaxSize()) {
        val route = current?.let { appRouteByPath[it.path] }
        if (route != null) {
            route.render(current.args)
        }
    }
}
