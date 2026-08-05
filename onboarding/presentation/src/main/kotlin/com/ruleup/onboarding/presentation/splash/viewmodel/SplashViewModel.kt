package com.ruleup.onboarding.presentation.splash.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.onboarding.domain.auth.SessionBootstrap
import com.ruleup.onboarding.domain.auth.SessionBootstrapState
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "[Splash]"

/**
 * 스플래시 ViewModel. 판정 자체는 하지 않고 [SessionBootstrap] 의 결과를 기다린다 —
 * 버전 게이트와 자동 로그인은 액티비티 `onCreate` 에서 이미 착수되어 컴포지션과 겹쳐 돌고 있다.
 *
 * **어디로 갈지는 여기서 정하지 않는다.** 진입 목적지는 [SessionBootstrap] 이 계산하고 네비게이션
 * 호스트가 옮긴다 — 판정에 필요한 건 세션·보류 딥링크·경로 공개 여부뿐이라 이 화면과 무관하다.
 * 스플래시가 갖는 건 버전 게이트 UI 상태 하나다.
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val sessionBootstrap: SessionBootstrap,
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
                is SplashReducerEvent.CheckFinished -> {
                    state.copy(isChecking = false)
                }

                is SplashReducerEvent.ForceUpdateRequired -> {
                    state.copy(isChecking = false, forceUpdate = true, minAppVersion = event.minAppVersion)
                }
            }

        private fun observeBootstrap() {
            viewModelScope.launch {
                // 판정이 이미 끝나 있으면 StateFlow 가 즉시 그 값을 준다 — 액티비티 재생성에도 안전하다.
                sessionBootstrap.state.collect { state ->
                    when (state) {
                        SessionBootstrapState.Running -> {
                            Unit
                        }

                        is SessionBootstrapState.ForceUpdate -> {
                            // devTestMsg 는 개발·점검용이라 사용자에게 노출하지 않는다. 운영 중
                            // 무슨 이유로 게이트가 걸렸는지는 로그로 남겨야 추적이 된다.
                            state.devTestMsg?.let { msg -> observability.i(TAG) { "강제 업데이트: $msg" } }
                            dispatch(SplashReducerEvent.ForceUpdateRequired(state.minAppVersion))
                            return@collect
                        }

                        is SessionBootstrapState.Resolved -> {
                            // 어디로 갈지는 SessionBootstrap 이 정하고 AppNavHost 가 옮긴다.
                            // 스플래시는 판정이 끝났다는 것만 안다.
                            dispatch(SplashReducerEvent.CheckFinished)
                            return@collect
                        }
                    }
                }
            }
        }
    }
