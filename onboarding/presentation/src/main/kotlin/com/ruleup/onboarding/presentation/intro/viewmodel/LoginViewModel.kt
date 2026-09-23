package com.ruleup.onboarding.presentation.intro.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.entity.user.FeatureCode
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.token.TokenRepository
import com.ruleup.logging.domain.BizLogger
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.account.AccountRestrictionProvider
import com.ruleup.onboarding.domain.auth.SignupSession
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.LoginOutcome
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.usecase.SocialLoginUseCase
import com.ruleup.onboarding.domain.logging.LoginEntryType
import com.ruleup.onboarding.domain.logging.OnboardingEvents
import com.ruleup.onboarding.domain.logging.SignupTimer
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.OnboardingNicknamePage
import com.ruleup.onboarding.presentation.common.toAuthFailureUi
import com.ruleup.profile.domain.navigation.AccountLockedPage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "[Login]"

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val socialLoginUseCase: SocialLoginUseCase,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
        private val observability: Observability,
        private val bizLogger: BizLogger,
        private val signupTimer: SignupTimer,
        private val tokenRepository: TokenRepository,
        private val signupSession: SignupSession,
        private val pendingDeepLink: PendingDeepLink,
        private val accountRestrictionProvider: AccountRestrictionProvider,
    ) : MviViewModel<LoginIntent, LoginState, LoginReducerEvent, LoginEffect>(LoginState.initial) {
        override fun onIntent(intent: LoginIntent) {
            when (intent) {
                is LoginIntent.Load -> {
                    dispatch(LoginReducerEvent.Loaded)
                    viewModelScope.launch {
                        // 완주율의 분모. 첫 설치와 재로그인을 나누지 않으면 분모가 뒤섞인다.
                        val entryType = if (tokenRepository.hasEverLoggedIn()) LoginEntryType.RELOGIN else LoginEntryType.FRESH
                        bizLogger.record(OnboardingEvents.loginScreenView(entryType))
                    }
                }

                is LoginIntent.LoginClicked -> {
                    dispatch(LoginReducerEvent.LoginStarted)
                    // 가입 소요 시간의 시작점. signup_complete 가 이 값과의 차이를 싣는다.
                    signupTimer.start()
                    bizLogger.record(OnboardingEvents.loginAttempt(intent.provider.provider))
                    emitEffect(LoginEffect.LaunchOAuth(intent.provider))
                }

                is LoginIntent.AuthorizationReceived -> {
                    socialLogin(authorization = intent.authorization)
                }

                is LoginIntent.AuthFailed -> {
                    // IdP 화면에서 사용자가 취소한 경우가 대부분이라 조용히 되돌린다.
                    dispatch(LoginReducerEvent.LoginFinished)
                }
            }
        }

        override fun reduce(
            state: LoginState,
            event: LoginReducerEvent,
        ): LoginState =
            when (event) {
                is LoginReducerEvent.Loaded -> {
                    state.copy(isLoading = false)
                }

                is LoginReducerEvent.LoginStarted -> {
                    state.copy(isLoading = true)
                }

                is LoginReducerEvent.LoginFinished -> {
                    state.copy(isLoading = false)
                }
            }

        /**
         * 제한 계정의 진입. 전체 잠금은 잠금 화면에 고정하고, 기능 정지는 그 기능만 막힌다는 것을
         * 알린 뒤 평소처럼 홈으로 보낸다.
         */
        private suspend fun enterRestricted(result: LoginOutcome.Restricted) {
            when (val restriction = accountRestrictionProvider.current()) {
                is AccountRestriction.Feature -> {
                    messageHelper.showSnackBar("${FeatureCode.label(restriction.featureCode)} 기능이 정지된 상태예요")
                    navigationHelper.goHomeOrPending(pendingDeepLink)
                }

                AccountRestriction.Locked, AccountRestriction.Banned ->
                    navigationHelper.navigateTo(AccountLockedPage)

                // 제재 조회가 실패했다. 잠금 화면에 가두지 않고 사유만 알린 뒤 들여보낸다 —
                // 막힌 기능은 각 화면이 서버 거절로 안내한다.
                AccountRestriction.None -> {
                    messageHelper.showSnackBar(
                        result.lockInfo?.let { "계정이 잠겨 있어요 (해제: ${it.unlockAt})" }
                            ?: "계정에 제한이 걸려 있어요",
                    )
                    navigationHelper.goHomeOrPending(pendingDeepLink)
                }
            }
        }

        private fun socialLogin(authorization: OAuthAuthorization) {
            viewModelScope.launch {
                runCatching {
                    socialLoginUseCase(authorization)
                }.onSuccess { result ->
                    dispatch(LoginReducerEvent.LoginFinished)
                    bizLogger.record(
                        OnboardingEvents.loginResult(
                            provider = authorization.provider.provider,
                            success = true,
                            isNewUser = result is LoginOutcome.GoSignup,
                            restored = (result as? LoginOutcome.GoHome)?.restored,
                        ),
                    )
                    when (result) {
                        is LoginOutcome.GoHome -> navigationHelper.goHomeOrPending(pendingDeepLink)

                        // 제한이 걸린 계정도 로그인은 된다. 어디까지 막혔는지는 응답이 말하지
                        // 않으므로 제재를 한 번 더 물어 **스플래시와 같은 기준으로** 가른다 —
                        // 여기서만 홈으로 보내면 로그아웃 후 재로그인이 잠금을 우회한다(AUTH-13).
                        is LoginOutcome.Restricted -> enterRestricted(result)

                        // 복원 중 닉네임을 선점당했다. 바꾸기 전엔 홈으로 보내지 않는다.
                        is LoginOutcome.ResetNickname -> {
                            messageHelper.showSnackBar(
                                messageText = "'${result.currentNickname}' 을(를) 다른 분이 쓰고 있어요. 새 닉네임을 정해주세요",
                            )
                            navigationHelper.navigateTo(OnboardingNicknamePage)
                        }

                        is LoginOutcome.GoSignup -> {
                            // 토큰은 백스택에 실지 않는다 — 직렬화되어 saved state 에 남는다.
                            signupSession.start(result.signupToken, result.profile)
                            navigationHelper.navigateTo(OnboardingNicknamePage)
                        }
                    }
                }.onFailure { error ->
                    dispatch(LoginReducerEvent.LoginFinished)
                    // 원인은 로그로만 남긴다 — 사용자가 고칠 수 없는 코드(redirectUri·deviceInfo)까지
                    // 화면에 드러내면 안내만 어지러워진다.
                    observability.w(TAG, error) { "소셜 로그인 실패" }
                    bizLogger.record(
                        OnboardingEvents.loginResult(
                            provider = authorization.provider.provider,
                            success = false,
                            errorCode = (error as? AuthException)?.failure?.name,
                        ),
                    )
                    emitEffect(LoginEffect.ShowFailure(error.toAuthFailureUi()))
                }
            }
        }
    }

/** 로그인·가입을 마친 사용자를 보관된 딥링크 목적지로, 없으면 홈으로 보낸다. */
internal fun NavigationHelper.goHomeOrPending(pendingDeepLink: PendingDeepLink) {
    pendingDeepLink.consumeAfterLogin()?.let(::replaceStackWith) ?: navigateTo(HomePage)
}
