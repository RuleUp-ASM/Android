package com.ruleup.onboarding.presentation.splash.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.PendingDeepLinkEntry
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.onboarding.domain.account.AccountRestrictionProvider
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginResult
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.intro.repository.WalkthroughRepository
import com.ruleup.onboarding.domain.intro.usecase.IntroGate
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.onboarding.domain.navigation.WalkthroughPage
import com.ruleup.profile.domain.navigation.AccountLockedPage
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
        private val accountRestrictionProvider: AccountRestrictionProvider,
        private val walkthroughRepository: WalkthroughRepository,
        private val tokenRepository: TokenRepository,
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

                // 다시 시도는 진입 절차를 처음부터 돌린다 — 버전 게이트도 같이 다시 본다.
                is SplashIntent.Retry -> {
                    started = false
                    resolveEntry()
                }
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

                is SplashReducerEvent.CheckStarted -> {
                    state.copy(isChecking = true, connectionFailed = false)
                }

                is SplashReducerEvent.ConnectionFailed -> {
                    state.copy(isChecking = false, connectionFailed = true)
                }
            }

        private fun resolveEntry() {
            if (started) return
            started = true
            dispatch(SplashReducerEvent.CheckStarted)
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
                        val login = autoLoginUseCase()
                        // 연결이 안 돼 세션을 확인하지 못했다. 로그인 화면으로 보내면 아직 며칠 남은
                        // 세션을 사용자가 버린 것처럼 보인다 — 여기 머문 채 다시 시도하게 한다(ENV-03).
                        if (login is AutoLoginResult.ConnectionFailed) {
                            observability.i(TAG) { "자동 로그인 전송 실패 — 세션은 유지하고 재시도를 기다린다" }
                            dispatch(SplashReducerEvent.ConnectionFailed)
                            return@launch
                        }
                        val authenticated = login is AutoLoginResult.Authenticated
                        dispatch(SplashReducerEvent.CheckFinished)
                        // 전체 잠금 계정은 어떤 화면도 열지 않는다 — 딥링크가 있어도 잠금이 먼저다.
                        // **기능 정지는 여기서 막지 않는다.** 정지된 기능은 그 기능만 막으면 되고,
                        // 앱 전체를 잠그면 신고 하나 막힌 계정이 아무것도 못 하게 된다.
                        if (authenticated) {
                            val restriction = accountRestrictionProvider.current()
                            if (restriction.isFullLock) {
                                observability.i(TAG) { "로그인 정지 — 잠금 화면으로 고정 진입" }
                                navigationHelper.navigateTo(AccountLockedPage)
                                return@launch
                            }
                            if (restriction is AccountRestriction.Feature) {
                                observability.i(TAG) { "기능 정지 — 진입은 통과: feature=${restriction.featureCode}" }
                            }
                        }
                        navigate(authenticated)
                    }
                }
            }
        }

        private suspend fun navigate(authenticated: Boolean) {
            when (val pending = pendingDeepLink.consumeFor(authenticated, routeAccessPolicy)) {
                // 딥링크는 부모 화면까지 함께 깔아야 뒤로가기가 자연스럽다(공지 상세 → 방 홈 → 홈).
                is PendingDeepLinkEntry.Open -> navigationHelper.replaceStackWith(pending.route)

                // 목적지는 보관된 채 남는다 — 로그인·가입을 마치면 그 화면이 연다.
                is PendingDeepLinkEntry.Deferred -> {
                    observability.i(TAG) { "인증 전 딥링크 보류: path=${pending.route.path}" }
                    navigationHelper.navigateTo(LoginPage)
                }

                // 홈·로그인·워크쓰루는 루트라 호스트가 스택을 비우고 단독으로 세운다.
                PendingDeepLinkEntry.None -> navigationHelper.navigateTo(if (authenticated) HomePage else guestEntry())
            }
        }

        /**
         * 로그인 안 된 사용자의 첫 화면. 소개를 아직 못 봤으면 워크쓰루가 먼저다.
         *
         * **로그인한 적이 있으면 워크쓰루로 보내지 않는다.** 세션이 끝나 돌아온 기존 가입자에게
         * "01 만들기" 소개를 다시 펴면 가입 전으로 되돌아간 것처럼 보인다(AUTH-05 · NAV-10).
         * 소개를 건너뛰고 로그인한 사용자는 `isSeen()` 이 false 라 이 조건이 따로 필요하다.
         *
         * 보류 딥링크가 있을 때는 이 경로로 오지 않는다 — 목적지가 있는 사용자를 소개로 붙잡으면
         * 링크를 타고 온 이유가 한 번 더 밀린다.
         */
        private suspend fun guestEntry() =
            if (walkthroughRepository.isSeen() || tokenRepository.hasEverLoggedIn()) LoginPage else WalkthroughPage
    }
