package com.ruleup.challenge.presentation.fake

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 테스트용 [NavigationHelper]. 어디로 갔는지만 남긴다. */
class RecordingNavigationHelper : NavigationHelper {
    val routes = mutableListOf<NavRoute>()
    var backCount = 0
        private set

    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) {
        routes += route
    }

    override fun navigateTo(page: Page) {
        routes += page.toRoute()
    }

    // 목록 화면이 백스택을 갈아치울 일은 없다 — 부르면 드러나야 한다.
    override fun replaceStackWith(route: NavRoute) = throw NotImplementedError()

    override fun navigateToBack() {
        backCount += 1
    }
}
