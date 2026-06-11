package com.ruleup.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavKey
import com.ruleup.onboarding.domain.SplashPage
import com.ruleup.shared.di.AppGraph
import com.ruleup.shared.navigation.GenericNavKey
import com.ruleup.shared.navigation.RootComposable
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

/**
 * 플랫폼 공통 앱 루트 컴포저블. 그래프에서 꺼낸 헬퍼/팩토리를 CompositionLocal 로 제공하고
 * [RootComposable] 을 그린다. Android(MainActivity)·iOS(MainViewController) 가 공통으로 호출한다.
 */
@Composable
fun AppRoot(
    graph: AppGraph,
    startStack: List<NavKey> = listOf(GenericNavKey(SplashPage.PATH)),
) {
    CompositionLocalProvider(
        LocalNavigationHelper provides graph.navigationHelper,
        LocalMessageHelper provides graph.messageHelper,
        LocalMetroViewModelFactory provides graph.metroViewModelFactory,
    ) {
        RootComposable(startStack = startStack)
    }
}
