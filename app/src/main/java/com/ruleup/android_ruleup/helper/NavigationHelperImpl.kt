package com.ruleup.android_ruleup.helper

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.DeeplinkResolver
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class NavigationHelperImpl
    @Inject
    constructor(
        private val deeplinkResolver: DeeplinkResolver,
    ) : NavigationHelper {
        private val _navigationFlow = Channel<NavSignal>(capacity = Channel.BUFFERED)
        override val navigationFlow: Flow<NavSignal> = _navigationFlow.receiveAsFlow()

        override fun navigateTo(page: Page) {
            navigateByRoute(page.toRoute())
        }

        override fun navigateByRoute(route: NavRoute) {
            emit(NavSignal.GoToDestPage(route))
        }

        override fun replaceStackWith(route: NavRoute) {
            emit(NavSignal.ReplaceStack(route))
        }

        override fun navigateByDeeplink(deeplink: String) {
            // 해석 못 하면 아무 일도 하지 않는다 — 서버가 타입을 늘리는 건 정상이고, 그때
            // 엉뚱한 화면으로 보내는 것보다 제자리에 두는 편이 낫다.
            deeplinkResolver.resolve(deeplink)?.let(::navigateByRoute)
        }

        override fun navigateToBack() {
            emit(NavSignal.Back)
        }

        private fun emit(navSignal: NavSignal) {
            val result = _navigationFlow.trySend(navSignal)
            if (result.isFailure) println("NavigationHelper dropped: $navSignal")
        }
    }
