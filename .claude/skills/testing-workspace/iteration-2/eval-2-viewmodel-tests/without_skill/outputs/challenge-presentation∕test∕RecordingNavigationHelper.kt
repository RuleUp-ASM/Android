package com.ruleup.challenge.presentation.fake

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 이동 요청을 [NavSignal] 로 받아 적는 대역. 호스트가 실제로 보게 될 형태 그대로 남기므로,
 * "무엇을 향해 이동했는가"를 경로 문자열이 아니라 신호 단위로 검증할 수 있다.
 */
class RecordingNavigationHelper : NavigationHelper {
    val signals = mutableListOf<NavSignal>()

    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) {
        signals += NavSignal.GoToDestPage(route)
    }

    override fun navigateTo(page: Page) {
        signals += NavSignal.GoToDestPage(page.toRoute())
    }

    override fun replaceStackWith(route: NavRoute) {
        signals += NavSignal.ReplaceStack(route)
    }

    override fun navigateToBack() {
        signals += NavSignal.Back
    }
}
