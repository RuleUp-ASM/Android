package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProvider
import com.ruleup.ui.mvi.MviIntent

sealed interface LoginIntent : MviIntent {
    /** 화면 진입. 첫 설치인지 재로그인인지는 부트스트랩이 안다. */
    data object Load : LoginIntent

    data class LoginClicked(
        val provider: OAuthProvider,
    ) : LoginIntent

    data class AuthorizationReceived(
        val authorization: OAuthAuthorization,
    ) : LoginIntent

    data class AuthFailed(
        val error: Throwable,
    ) : LoginIntent
}
