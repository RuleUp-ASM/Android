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
 * 이 앱의 화면 이동은 이펙트가 아니라 협력자 호출이라, 대역이 없으면 **실패했는데 다음 화면으로
 * 넘어갔다**를 어떤 ViewModel 테스트도 잡을 수 없다. 그래서 간 곳뿐 아니라 "아무 데도 안 갔다"도
 * 볼 수 있게 기록만 하고 아무것도 하지 않는다.
 */
class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    val routes = mutableListOf<NavRoute>()
    val replaced = mutableListOf<NavRoute>()

    var backCount: Int = 0
        private set

    /** 이동이 한 번도 없었는가. 실패 경로에서 자주 쓴다. */
    val didNotMove: Boolean
        get() = pages.isEmpty() && routes.isEmpty() && replaced.isEmpty() && backCount == 0

    /** 마지막으로 요청한 경로 path. [navigateTo] 로 간 경우도 포함해 보려면 [pages] 를 직접 본다. */
    val lastRoutePath: String?
        get() = routes.lastOrNull()?.path

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
