package com.ruleup.android_ruleup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavKey
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.navigation.RootComposable
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.navigation.SplashPage
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 앱 루트 컴포저블. Hilt 가 주입한 헬퍼들을 CompositionLocal 로 제공하고 [RootComposable] 을 그린다.
 * @AndroidEntryPoint MainActivity 가 헬퍼를 주입받아 호출한다. ViewModel 은 hiltViewModel() 이 직접 해결한다.
 */
@Composable
fun AppRoot(
    navigationHelper: NavigationHelper,
    messageHelper: MessageHelper,
    analyticsLogger: AnalyticsLogger,
    startStack: List<NavKey> = listOf(GenericNavKey(SplashPage.PATH)),
) {
    CompositionLocalProvider(
        LocalNavigationHelper provides navigationHelper,
        LocalMessageHelper provides messageHelper,
        LocalAnalyticsLogger provides analyticsLogger,
    ) {
        RootComposable(startStack = startStack)
    }
}
