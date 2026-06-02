package com.ruleup.presentation.auth.login.vm

import androidx.lifecycle.viewModelScope
import com.ruleup.core.ui.mvi.MviViewModel
import com.ruleup.domain.auth.model.OAuthResult
import com.ruleup.domain.auth.usecase.AuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authUseCase: AuthUseCase,
    ) : MviViewModel<LoginIntent, LoginState, LoginReducerEvent, LoginEffect>(LoginState.initial) {
        init {
            onIntent(LoginIntent.Load)
        }

        override fun onIntent(intent: LoginIntent) {
            when (intent) {
                is LoginIntent.Load -> {
                    dispatch(LoginReducerEvent.Loaded)
                }

                is LoginIntent.Login -> {
                    dispatch(LoginReducerEvent.LoginStarted(intent.title))
                    viewModelScope.launch {
                        runCatching {
                            authUseCase.login(provider = intent.title)
                        }.onSuccess { result ->
                            when (result) {
                                is OAuthResult.ExistingUser -> {
                                    dispatch(LoginReducerEvent.LoginSucceeded)
                                    emitEffect(LoginEffect.NavigateToHome)
                                }

                                is OAuthResult.NewUser -> {
                                    dispatch(LoginReducerEvent.SignupRequired)
                                    emitEffect(
                                        LoginEffect.NavigateToSignup(
                                            signupToken = result.signupToken,
                                            oauthProfile = result.oauthProfile,
                                        ),
                                    )
                                }
                            }
                        }.onFailure { throwable ->
                            dispatch(LoginReducerEvent.LoginFailed(throwable))
                            emitEffect(LoginEffect.ShowError(throwable.message ?: "로그인에 실패했어요"))
                        }
                    }
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
                    state.copy(provider = event.provider, isLoading = true)
                }

                is LoginReducerEvent.LoginSucceeded -> {
                    state.copy(isLoading = false)
                }

                is LoginReducerEvent.SignupRequired -> {
                    state.copy(isLoading = false)
                }

                is LoginReducerEvent.LoginFailed -> {
                    state.copy(isLoading = false)
                }
            }
    }
