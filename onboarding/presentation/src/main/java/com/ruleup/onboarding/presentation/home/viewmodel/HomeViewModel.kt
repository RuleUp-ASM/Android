package com.ruleup.onboarding.presentation.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.onboarding.domain.LoginPage
import com.ruleup.onboarding.domain.auth.usecase.LogoutUseCase
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HomeViewModel
    constructor(
        private val logoutUseCase: LogoutUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<HomeIntent, HomeState, HomeReducerEvent, NoEffect>(HomeState.initial) {
        override fun onIntent(intent: HomeIntent) {
            when (intent) {
                HomeIntent.CreateChallenge -> {
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_CREATE))
                }

                HomeIntent.Logout -> {
                    logout()
                }
            }
        }

        override fun reduce(
            state: HomeState,
            event: HomeReducerEvent,
        ): HomeState =
            when (event) {
                HomeReducerEvent.LogoutStarted -> {
                    state.copy(isLoggingOut = true)
                }
            }

        private fun logout() {
            if (currentState.isLoggingOut) return
            viewModelScope.launch {
                dispatch(HomeReducerEvent.LogoutStarted)
                logoutUseCase()
                navigationHelper.navigateTo(LoginPage)
            }
        }
    }
