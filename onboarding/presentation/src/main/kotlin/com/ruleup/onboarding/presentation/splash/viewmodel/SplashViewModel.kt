package com.ruleup.onboarding.presentation.splash.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.HomePage
import com.ruleup.onboarding.domain.IntroPromisePage
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 스플래시 ViewModel. 진입 시 [AutoLoginUseCase] 로 세션을 복구한 뒤,
 * 성공하면 홈으로(루트), 실패하면 온보딩 인트로로(루트) 이동한다.
 * 이동은 [NavigationHelper] 사이드 이펙트로 처리하므로 별도 MVI 이펙트는 두지 않는다([NoEffect]).
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val autoLoginUseCase: AutoLoginUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<SplashIntent, SplashState, SplashReducerEvent, NoEffect>(SplashState.initial) {
        override fun onIntent(intent: SplashIntent) {
            when (intent) {
                is SplashIntent.Check -> checkAutoLogin()
            }
        }

        override fun reduce(
            state: SplashState,
            event: SplashReducerEvent,
        ): SplashState =
            when (event) {
                is SplashReducerEvent.CheckFinished -> state.copy(isChecking = false)
            }

        private fun checkAutoLogin() {
            viewModelScope.launch {
                val authenticated = autoLoginUseCase()
                dispatch(SplashReducerEvent.CheckFinished)
                if (authenticated) {
                    navigationHelper.navigateTo(HomePage)
                } else {
                    navigationHelper.navigateTo(IntroPromisePage)
                }
            }
        }
    }
