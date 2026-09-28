package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.onboarding.domain.auth.entity.OAuthProvider
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.ui.mvi.MviEffect

sealed interface LoginEffect : MviEffect {
    data class LaunchOAuth(
        val provider: OAuthProvider,
    ) : LoginEffect

    /** 실패 안내. */
    data class ShowFailure(
        val ui: AuthFailureUi,
    ) : LoginEffect
}
