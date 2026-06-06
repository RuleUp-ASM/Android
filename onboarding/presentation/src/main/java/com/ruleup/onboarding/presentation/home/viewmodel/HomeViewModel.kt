package com.ruleup.onboarding.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.auth.usecase.LogoutUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.LoginPage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val logoutUseCase: LogoutUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<HomeIntent, HomeState, HomeReducerEvent, NoEffect>(HomeState.initial) {
        override fun onIntent(intent: HomeIntent) {
            when (intent) {
                HomeIntent.CreateChallenge -> Unit // TODO: 챌린지 생성 화면이 정의되면 연결한다.

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
