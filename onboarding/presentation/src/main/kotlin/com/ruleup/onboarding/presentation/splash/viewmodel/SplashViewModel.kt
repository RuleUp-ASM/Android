package com.ruleup.onboarding.presentation.splash.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.PendingDeepLinkEntry
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.intro.usecase.IntroGate
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "[Splash]"

/**
 * 앱 진입 절차를 실행한다 — 버전 게이트 → 자동 로그인 → 목적지.
 *
 * 판정은 하나도 여기서 하지 않는다 — 게이트는 [LoadIntroUseCase], 세션 복구는 [AutoLoginUseCase],
 * 보류 딥링크는 [PendingDeepLink.consumeFor] 가 정하고, 이 화면은 셋을 부르는 순서만 갖는다.
 *
 * **백스택은 세우지 않는다.** 목적지만 [NavigationHelper] 로 흘려보낸다 — 딥링크는 부모 화면까지
 * 함께 깔아야 뒤로가기가 자연스럽고, 그건 라우트 목록을 가진 호스트만 할 수 있다.
 *
 * 세션이 끊겨 이 화면으로 되돌아오면 새 인스턴스가 절차를 처음부터 다시 돈다. 재판정을 위한 별도
 * 경로가 필요 없는 이유다.
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val loadIntroUseCase: LoadIntroUseCase,
        private val autoLoginUseCase: AutoLoginUseCase,
        private val pendingDeepLink: PendingDeepLink,
        private val routeAccessPolicy: RouteAccessPolicy,
        private val navigationHelper: NavigationHelper,
        private val observability: Observability,
    ) : MviViewModel<SplashIntent, SplashState, SplashReducerEvent, NoEffect>(SplashState.initial) {
        /**
         * 액티비티가 재생성되면 컴포지션이 다시 만들어져 진입 인텐트가 한 번 더 들어온다. ViewModel 은
         * 살아남으므로, 막지 않으면 인트로 조회와 토큰 재발급이 두 번 나간다.
         */
        private var started = false

        override fun onIntent(intent: SplashIntent) {
            when (intent) {
                is SplashIntent.Check -> resolveEntry()
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

        private fun resolveEntry() {
            if (started) return
            started = true
            viewModelScope.launch {
                // 버전 게이트가 먼저다. 걸리면 자동 로그인도 하지 않는다 — 업데이트 전에는 어떤
                // 화면도 열지 않으므로 세션을 되살릴 이유가 없다.
                when (val gate = loadIntroUseCase()) {
                    is IntroGate.ForceUpdate -> {
                        // devTestMsg 는 개발·점검용이라 사용자에게 노출하지 않는다. 운영 중 무슨
                        // 이유로 게이트가 걸렸는지는 로그로 남겨야 추적이 된다.
                        gate.devTestMsg?.let { msg -> observability.i(TAG) { "강제 업데이트: $msg" } }
                        dispatch(SplashReducerEvent.ForceUpdateRequired(gate.minAppVersion))
                    }

                    IntroGate.Pass -> {
                        val authenticated = autoLoginUseCase()
                        dispatch(SplashReducerEvent.CheckFinished)
                        navigate(authenticated)
                    }
                }
            }
        }

        private fun navigate(authenticated: Boolean) {
            when (val pending = pendingDeepLink.consumeFor(authenticated, routeAccessPolicy)) {
                // 딥링크는 부모 화면까지 함께 깔아야 뒤로가기가 자연스럽다(공지 상세 → 방 홈 → 홈).
                is PendingDeepLinkEntry.Open -> navigationHelper.replaceStackWith(pending.route)

                is PendingDeepLinkEntry.Dropped -> {
                    // 초대 링크로 유입된 신규 사용자가 여기 걸린다. 가입을 마쳐도 목적지로 돌아가지
                    // 않으므로, 잃어버린 진입을 집계해 이어가기 필요성을 판단할 근거를 남긴다.
                    observability.w(TAG) { "인증 전이라 딥링크 유실: path=${pending.route.path}" }
                    navigationHelper.navigateTo(LoginPage)
                }

                // 홈·로그인은 루트라 호스트가 스택을 비우고 단독으로 세운다.
                PendingDeepLinkEntry.None -> navigationHelper.navigateTo(if (authenticated) HomePage else LoginPage)
            }
        }
    }
