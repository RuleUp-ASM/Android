package com.ruleup.domain.test

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 이동 요청을 순서대로 모아두는 [NavigationHelper]. */
class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    val routes = mutableListOf<NavRoute>()
    val replaced = mutableListOf<NavRoute>()

    /** 서버가 준 딥링크 문자열. */
    val deeplinks = mutableListOf<String>()

    var backCount: Int = 0
        private set

    /** 이동이 한 번도 없었는가. */
    val didNotMove: Boolean
        get() = pages.isEmpty() && routes.isEmpty() && replaced.isEmpty() && deeplinks.isEmpty() && backCount == 0

    /** 마지막으로 요청한 경로 path. */
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

    override fun navigateByDeeplink(deeplink: String) {
        deeplinks += deeplink
    }

    override fun navigateToBack() {
        backCount++
    }
}
