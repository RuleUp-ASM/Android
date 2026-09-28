package com.ruleup.domain.helper

import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow

/** 단일 네비게이션 플로우. */
interface NavigationHelper {
    val navigationFlow: Flow<NavSignal>

    fun navigateByRoute(route: NavRoute)

    fun navigateTo(page: Page)

    /** 백스택을 [route] 의 시작 스택으로 교체한다. */
    fun replaceStackWith(route: NavRoute)

    /** 서버가 준 딥링크(`ruleup://…`)로 이동한다. */
    fun navigateByDeeplink(deeplink: String)

    fun navigateToBack()
}
