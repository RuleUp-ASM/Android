package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.MviIntent

sealed interface SplashIntent : MviIntent {
    /** 화면 진입 시 자동 로그인을 시도한다. */
    data object Check : SplashIntent
}
