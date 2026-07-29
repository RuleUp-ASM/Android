package com.ruleup.onboarding.presentation.splash.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.SessionBootstrap
import com.ruleup.onboarding.domain.auth.SessionBootstrapState
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.IntroPromisePage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.navigation.PendingDeepLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "[Splash]"

/**
 * 스플래시 ViewModel. 판정 자체는 하지 않고 [SessionBootstrap] 의 결과를 기다린다 —
 * 버전 게이트와 자동 로그인은 액티비티 `onCreate` 에서 이미 착수되어 컴포지션과 겹쳐 돌고 있다.
 *
 * 인증이 끝나면 목적지를 정한다:
 * - 보류된 딥링크가 있으면 그 화면의 시작 스택으로 **교체**한다. 전진 이동으로 처리하면 스플래시가
 *   스택에 남아, 뒤로 갔을 때 판정이 다시 돌아간다.
 * - 없으면 홈(루트).
 * - 인증 실패면 인트로(루트). 보류 딥링크는 버려진다.
 *
 * 이동은 [NavigationHelper] 사이드 이펙트로 처리하므로 별도 MVI 이펙트는 두지 않는다([NoEffect]).
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val sessionBootstrap: SessionBootstrap,
        private val pendingDeepLink: PendingDeepLink,
        private val navigationHelper: NavigationHelper,
        private val observability: Observability,
    ) : MviViewModel<SplashIntent, SplashState, SplashReducerEvent, NoEffect>(SplashState.initial) {
        override fun onIntent(intent: SplashIntent) {
            when (intent) {
                is SplashIntent.Check -> observeBootstrap()
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

        private fun observeBootstrap() {
            viewModelScope.launch {
                // 판정이 이미 끝나 있으면 StateFlow 가 즉시 그 값을 준다 — 액티비티 재생성에도 안전하다.
                sessionBootstrap.state.collect { state ->
                    when (state) {
                        SessionBootstrapState.Running -> Unit
                        is SessionBootstrapState.ForceUpdate -> {
                            dispatch(SplashReducerEvent.ForceUpdateRequired(state.message))
                            return@collect
                        }
                        is SessionBootstrapState.Resolved -> {
                            dispatch(SplashReducerEvent.CheckFinished)
                            route(state.authenticated)
                            return@collect
                        }
                    }
                }
            }
        }

        private fun route(authenticated: Boolean) {
            val pending = pendingDeepLink.consume()
            when {
                !authenticated -> {
                    if (pending != null) {
                        // 초대 링크로 유입된 신규 사용자가 여기 걸린다. 가입을 마쳐도 목적지로 돌아가지
                        // 않으므로, 잃어버린 진입을 집계해 이어가기 필요성을 판단할 근거를 남긴다.
                        observability.w(TAG) { "인증 전이라 딥링크 유실: path=${pending.path}" }
                    }
                    navigationHelper.navigateTo(IntroPromisePage)
                }
                pending != null -> navigationHelper.replaceStackWith(pending)
                else -> navigationHelper.navigateTo(HomePage)
            }
        }
    }
