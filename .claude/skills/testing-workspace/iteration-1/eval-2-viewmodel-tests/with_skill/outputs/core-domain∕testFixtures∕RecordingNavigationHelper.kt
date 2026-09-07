package com.ruleup.domain.test

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 이동 요청을 순서대로 모아두는 [NavigationHelper].
 *
 * 이 레포에서 화면 이동은 Effect 가 아니라 협력자 호출이라, 대역이 없으면 "실패했는데 다음 화면으로
 * 넘어갔다"를 어떤 ViewModel 테스트도 잡지 못한다. **안 갔다**를 단언하는 것도 이 대역의 용도다.
 */
class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    val routes = mutableListOf<NavRoute>()
    val replaced = mutableListOf<NavRoute>()
    var backCount = 0
        private set

    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) {
        routes += route
    }

    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun replaceStackWith(route: NavRoute) {
        replaced += route
    }

    override fun navigateToBack() {
        backCount++
    }
}
