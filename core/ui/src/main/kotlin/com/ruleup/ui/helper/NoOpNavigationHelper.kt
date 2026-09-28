package com.ruleup.ui.helper

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** `@Preview` 전용 [NavigationHelper] */
object NoOpNavigationHelper : NavigationHelper {
    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) = Unit

    override fun navigateTo(page: Page) = Unit

    override fun replaceStackWith(route: NavRoute) = Unit

    override fun navigateByDeeplink(deeplink: String) = Unit

    override fun navigateToBack() = Unit
}
