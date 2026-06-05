package com.ruleup.ui.helper

import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class NavigationHelperImpl : NavigationHelper {
    private val _navigationFlow = Channel<NavSignal>(capacity = Channel.BUFFERED)
    override val navigationFlow: Flow<NavSignal> = _navigationFlow.receiveAsFlow()

    override fun navigateTo(page: Page) {
        navigateByRoute(page.toRoute())
    }

    override fun navigateByRoute(route: NavRoute) {
        emit(NavSignal.GoToDestPage(route))
    }

    override fun navigateToBack() {
        emit(NavSignal.Back)
    }

    private fun emit(navSignal: NavSignal) {
        val result = _navigationFlow.trySend(navSignal)
        if (result.isFailure) Log.w("NavigationHelper", "dropped: $navSignal")
    }
}

val LocalNavigationHelper = compositionLocalOf<NavigationHelper> { error("No user found!") }
