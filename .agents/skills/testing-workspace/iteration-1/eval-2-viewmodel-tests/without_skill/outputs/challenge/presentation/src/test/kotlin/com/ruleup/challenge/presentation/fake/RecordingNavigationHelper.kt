package com.ruleup.challenge.presentation.fake

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 테스트용 [NavigationHelper]. 어디로 보냈는지만 기록한다. */
class RecordingNavigationHelper : NavigationHelper {
    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    private val recordedRoutes = mutableListOf<NavRoute>()
    private val recordedPages = mutableListOf<Page>()

    val routes: List<NavRoute> get() = recordedRoutes.toList()
    val pages: List<Page> get() = recordedPages.toList()

    var replacedStackWith: NavRoute? = null
        private set

    var backCount: Int = 0
        private set

    override fun navigateByRoute(route: NavRoute) {
        recordedRoutes += route
    }

    override fun navigateTo(page: Page) {
        recordedPages += page
    }

    override fun replaceStackWith(route: NavRoute) {
        replacedStackWith = route
    }

    override fun navigateToBack() {
        backCount++
    }
}
