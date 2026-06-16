package com.ruleup.shared.navigation

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ruleup.onboarding.domain.IntroPromisePage
import com.ruleup.ui.helper.LocalNavigationHelper

@Composable
actual fun PlatformNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        // 단일 GenericNavKey 디스패처. 실제 화면 결정은 [appRouteByPath] 가 담당한다.
        entryProvider =
            entryProvider {
                entry<GenericNavKey> { navKey ->
                    val route = appRouteByPath[navKey.path]
                    if (route == null) {
                        Log.w("[Navigation]", "Unknown path on render: ${navKey.path}")
                        LocalNavigationHelper.current.navigateTo(IntroPromisePage)
                        return@entry
                    }
                    route.render(navKey.args)
                }
            },
    )
}
