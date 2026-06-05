package com.ruleup.presentation.intro.viewmodel

import com.ruleup.entity.onboarding.OAuthProvider
import com.ruleup.ui.mvi.MviEffect

sealed interface LoginEffect : MviEffect {
    data class LaunchOAuth(
        val provider: OAuthProvider,
    ) : LoginEffect
}
