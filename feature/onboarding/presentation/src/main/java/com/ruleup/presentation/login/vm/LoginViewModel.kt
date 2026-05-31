package com.ruleup.presentation.login.vm

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.domain.auth.usecase.AuthUseCase
import com.ruleup.domain.model.OAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authUseCase: AuthUseCase,
    ) : ViewModel() {
        fun login(
            activity: Activity,
            provider: OAuthProvider,
        ) {
            viewModelScope.launch {
                authUseCase.login(
                    activity = activity,
                    provider = provider,
                )
            }
        }
    }
