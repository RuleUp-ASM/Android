package com.ruleup.presentation.auth.login.vm

import com.ruleup.core.ui.mvi.ReducerEvent
import com.ruleup.domain.auth.model.OAuthProvider

sealed interface LoginReducerEvent : ReducerEvent {
    data object Loaded : LoginReducerEvent

    data class LoginStarted(
        val provider: OAuthProvider,
    ) : LoginReducerEvent

    /** 기존 사용자: 로그인 완료. */
    data object LoginSucceeded : LoginReducerEvent

    /** 신규 사용자: 인가는 끝났고 가입 화면으로 넘어간다. */
    data object SignupRequired : LoginReducerEvent

    data class LoginFailed(
        val throwable: Throwable,
    ) : LoginReducerEvent
}
