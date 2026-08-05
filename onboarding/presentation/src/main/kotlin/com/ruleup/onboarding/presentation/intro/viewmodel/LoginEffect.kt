package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.onboarding.domain.entity.OAuthProvider
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.ui.mvi.MviEffect

sealed interface LoginEffect : MviEffect {
    data class LaunchOAuth(
        val provider: OAuthProvider,
    ) : LoginEffect

    /** 실패 안내. 무게(토스트·다이얼로그·전체 화면)는 [AuthFailureUi] 가 정한다. */
    data class ShowFailure(
        val ui: AuthFailureUi,
    ) : LoginEffect
}
