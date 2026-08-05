package com.ruleup.onboarding.presentation.intro.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.message.IconType
import com.ruleup.onboarding.domain.auth.usecase.SocialLoginUseCase
import com.ruleup.onboarding.domain.entity.LoginOutcome
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.ProfileIconPage
import com.ruleup.onboarding.domain.navigation.ProfileNicknamePage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val socialLoginUseCase: SocialLoginUseCase,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
    ) : MviViewModel<LoginIntent, LoginState, LoginReducerEvent, LoginEffect>(LoginState.initial) {
        override fun onIntent(intent: LoginIntent) {
            when (intent) {
                is LoginIntent.Load -> {
                    dispatch(LoginReducerEvent.Loaded)
                }

                is LoginIntent.LoginClicked -> {
                    dispatch(LoginReducerEvent.LoginStarted)
                    emitEffect(LoginEffect.LaunchOAuth(intent.provider))
                }

                is LoginIntent.AuthorizationReceived -> {
                    socialLogin(authorization = intent.authorization)
                }

                is LoginIntent.AuthFailed -> {
                    dispatch(LoginReducerEvent.LoginFinished)
                    messageHelper.showSnackBar(IconType.ERROR, intent.error.message ?: "로그인 취소")
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

        private fun socialLogin(authorization: OAuthAuthorization) {
            viewModelScope.launch {
                runCatching {
                    socialLoginUseCase(authorization)
                }.onSuccess { result ->
                    dispatch(LoginReducerEvent.LoginFinished)
                    when (result) {
                        LoginOutcome.GoHome -> navigationHelper.navigateTo(HomePage)

                        // 잠금 계정도 로그인은 된다. 홈은 열되 잠금 사유를 알려 준다 —
                        // 편집 등 막힌 기능은 각 화면이 ACCOUNT_LOCKED 로 안내한다.
                        is LoginOutcome.GoHomeReadOnly -> {
                            messageHelper.showSnackBar(
                                iconType = IconType.ERROR,
                                messageText =
                                    result.lockInfo?.let { "계정이 잠겨 있어요 (해제: ${it.unlockAt})" }
                                        ?: "계정이 잠겨 열람만 가능해요",
                            )
                            navigationHelper.navigateTo(HomePage)
                        }

                        // 복원 중 닉네임을 선점당했다. 바꾸기 전엔 홈으로 보내지 않는다.
                        is LoginOutcome.ResetNickname -> {
                            messageHelper.showSnackBar(
                                iconType = IconType.ERROR,
                                messageText = "'${result.currentNickname}' 을(를) 다른 분이 쓰고 있어요. 새 닉네임을 정해주세요",
                            )
                            navigationHelper.navigateTo(ProfileNicknamePage)
                        }

                        is LoginOutcome.GoSignup ->
                            navigationHelper.navigateByRoute(ProfileIconPage.routeWithToken(result.signupToken))
                    }
                }.onFailure { error ->
                    dispatch(LoginReducerEvent.LoginFinished)
                    messageHelper.showSnackBar(
                        iconType = IconType.ERROR,
                        messageText = error.message ?: "로그인 오류",
                    )
                }
            }
        }
    }
