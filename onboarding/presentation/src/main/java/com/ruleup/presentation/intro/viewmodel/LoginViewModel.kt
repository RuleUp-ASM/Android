package com.ruleup.presentation.intro.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.ProfileIconPage
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.message.IconType
import com.ruleup.domain.onboarding.SocialLoginUseCase
import com.ruleup.entity.onboarding.LoginResult
import com.ruleup.entity.onboarding.OAuthAuthorization
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
                        is LoginResult.GoMain -> {}

                        is LoginResult.GoSignup -> {
                            navigationHelper.navigateByRoute(ProfileIconPage.routeWithToken(result.signupToken))
                        }
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
