package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.ReducerEvent

sealed interface SplashReducerEvent : ReducerEvent {
    data object CheckFinished : SplashReducerEvent

    /** 강제 업데이트 필요 — 자동 로그인/이동을 멈추고 업데이트 화면을 노출한다. */
    data class ForceUpdateRequired(
        val message: String?,
    ) : SplashReducerEvent
}
