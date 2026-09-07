package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.ui.mvi.MviEffect

sealed interface OnboardingEffect : MviEffect {
    /** 실패 안내. 무게(토스트·다이얼로그·전체 화면)는 [AuthFailureUi] 가 정한다. */
    data class ShowFailure(
        val ui: AuthFailureUi,
    ) : OnboardingEffect

    /** 1단계 이탈 확인. 확인하면 로그인으로 돌아간다. */
    data object ConfirmExit : OnboardingEffect
}
