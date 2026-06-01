package com.ruleup.presentation.login.vm

import com.ruleup.core.ui.mvi.MviEffect
import com.ruleup.domain.model.OAuthProfile

sealed interface LoginEffect : MviEffect {
    /** 기존 사용자: 로그인 완료 → 홈으로 이동. */
    data object NavigateToHome : LoginEffect

    /** 신규 사용자: 가입 토큰/프로필을 들고 가입 화면으로 이동. */
    data class NavigateToSignup(
        val signupToken: String,
        val oauthProfile: OAuthProfile,
    ) : LoginEffect

    /** 인가/로그인 실패 안내. */
    data class ShowError(
        val message: String,
    ) : LoginEffect
}
