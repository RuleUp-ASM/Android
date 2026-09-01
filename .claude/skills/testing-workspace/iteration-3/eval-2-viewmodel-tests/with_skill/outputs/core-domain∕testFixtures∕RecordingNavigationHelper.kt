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
 * 이동은 이펙트가 아니라 협력자 호출이라 상태만 봐서는 "갔다/안 갔다"를 알 수 없다.
 * 실패했는데 다음 화면으로 넘어가는 건 사용자가 가장 크게 다치는 버그라, **안 간 것도** 단언할 수 있어야 한다.
 */
class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    val routes = mutableListOf<NavRoute>()
    val replaced = mutableListOf<NavRoute>()
    var backCount = 0
        private set

    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun navigateByRoute(route: NavRoute) {
        routes += route
    }

    override fun replaceStackWith(route: NavRoute) {
        replaced += route
    }

    override fun navigateToBack() {
        backCount++
    }
}
