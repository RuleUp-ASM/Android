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
 * 이 레포의 ViewModel 은 화면 이동을 이펙트가 아니라 협력자 호출로 한다 — 기록하는 대역이 없으면
 * "실패했는데도 다음 화면으로 넘어갔다"를 아무 테스트도 잡지 못한다. **어디로 갔는지만큼 안 갔는지도
 * 계약**이라 각 경로를 리스트로 남긴다.
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
