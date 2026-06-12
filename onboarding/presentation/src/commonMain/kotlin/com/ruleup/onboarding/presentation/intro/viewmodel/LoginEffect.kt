package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.onboarding.domain.entity.OAuthProvider
import com.ruleup.ui.mvi.MviEffect

sealed interface LoginEffect : MviEffect {
    data class LaunchOAuth(
        val provider: OAuthProvider,
    ) : LoginEffect
}
