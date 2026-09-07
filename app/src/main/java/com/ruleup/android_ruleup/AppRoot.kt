package com.ruleup.android_ruleup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.navigation.RootComposable
import com.ruleup.android_ruleup.observability.ScreenTracker
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.onboarding.domain.navigation.SplashPage
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.LocalObservability

/**
 * 앱 루트 컴포저블. 헬퍼는 `@AndroidEntryPoint` MainActivity 가 주입받아 여기로 넘긴다.
 * ViewModel 은 이 경로를 타지 않는다 — `hiltViewModel()` 이 직접 해결한다.
 */
@Composable
fun AppRoot(
    navigationHelper: NavigationHelper,
    messageHelper: MessageHelper,
    screenTracker: ScreenTracker,
    observability: Observability,
    startStack: List<NavKey> = listOf(GenericNavKey(SplashPage.PATH)),
) {
    CompositionLocalProvider(
        LocalNavigationHelper provides navigationHelper,
        LocalMessageHelper provides messageHelper,
        LocalScreenTracker provides screenTracker,
        LocalObservability provides observability,
    ) {
        RootComposable(startStack = startStack)
    }
}
