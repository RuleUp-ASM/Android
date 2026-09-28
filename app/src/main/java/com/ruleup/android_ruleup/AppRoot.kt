package com.ruleup.android_ruleup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.navigation.RootComposable
import com.ruleup.android_ruleup.observability.ScreenTracker
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.logging.domain.BizLogger
import com.ruleup.observability.domain.api.Observability
import com.ruleup.onboarding.domain.navigation.SplashPage
import com.ruleup.tti.domain.TtiRecorder
import com.ruleup.tti.presentation.LocalTtiRecorder
import com.ruleup.ui.helper.LocalBizLogger
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.LocalObservability

/** 앱 루트 컴포저블. */
@Composable
fun AppRoot(
    navigationHelper: NavigationHelper,
    messageHelper: MessageHelper,
    screenTracker: ScreenTracker,
    observability: Observability,
    bizLogger: BizLogger,
    ttiRecorder: TtiRecorder,
    startStack: List<NavKey> = listOf(GenericNavKey(SplashPage.PATH)),
) {
    CompositionLocalProvider(
        LocalNavigationHelper provides navigationHelper,
        LocalMessageHelper provides messageHelper,
        LocalScreenTracker provides screenTracker,
        LocalObservability provides observability,
        LocalBizLogger provides bizLogger,
        LocalTtiRecorder provides ttiRecorder,
    ) {
        RootComposable(startStack = startStack)
    }
}
