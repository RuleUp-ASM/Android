package com.ruleup.onboarding.presentation.splash.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.intro.usecase.CheckAppVersionUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.IntroPromisePage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 스플래시 ViewModel. 진입 시 (1) 앱 버전 게이트(GET /v1/intro)를 먼저 확인하고,
 * 강제 업데이트가 필요하면 더 진행하지 않고 업데이트 화면을 노출한다.
 * 그렇지 않으면 (2) [AutoLoginUseCase] 로 세션을 복구해 성공 시 홈으로(루트), 실패 시 인트로로(루트) 이동한다.
 * 이동은 [NavigationHelper] 사이드 이펙트로 처리하므로 별도 MVI 이펙트는 두지 않는다([NoEffect]).
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val checkAppVersionUseCase: CheckAppVersionUseCase,
        private val autoLoginUseCase: AutoLoginUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<SplashIntent, SplashState, SplashReducerEvent, NoEffect>(SplashState.initial) {
        override fun onIntent(intent: SplashIntent) {
            when (intent) {
                is SplashIntent.Check -> check()
            }
        }

        override fun reduce(
            state: SplashState,
            event: SplashReducerEvent,
        ): SplashState =
            when (event) {
                is SplashReducerEvent.CheckFinished -> state.copy(isChecking = false)
                is SplashReducerEvent.ForceUpdateRequired ->
                    state.copy(isChecking = false, forceUpdate = true, updateMessage = event.message)
            }

        private fun check() {
            viewModelScope.launch {
                // 1) 버전 게이트. 페일오픈(null)이면 정상 흐름을 그대로 진행한다.
                val gate = checkAppVersionUseCase()
                if (gate?.forceUpdate == true) {
                    dispatch(SplashReducerEvent.ForceUpdateRequired(gate.devTestMsg))
                    return@launch
                }

                // 2) 자동 로그인.
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
