package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.ui.mvi.MviEffect

sealed interface OnboardingEffect : MviEffect {
    /** 실패 안내. */
    data class ShowFailure(
        val ui: AuthFailureUi,
    ) : OnboardingEffect

    /** 1단계 이탈 확인. */
    data object ConfirmExit : OnboardingEffect
}
