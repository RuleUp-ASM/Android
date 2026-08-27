package com.ruleup.ui.helper

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * `@Preview` 전용 [NavigationHelper] — 실제 구현체는 `:app` 에 있어 feature 에서 참조할 수 없다.
 * 프리뷰에서 버튼을 눌러도 아무 일이 없는 건 의도된 동작이다.
 */
object NoOpNavigationHelper : NavigationHelper {
    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) = Unit

    override fun navigateTo(page: Page) = Unit

    override fun replaceStackWith(route: NavRoute) = Unit

    override fun navigateToBack() = Unit
}
