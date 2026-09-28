package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.ReducerEvent

sealed interface SplashReducerEvent : ReducerEvent {
    data object CheckFinished : SplashReducerEvent

    /** 강제 업데이트 필요 */
    data class ForceUpdateRequired(
        val minAppVersion: String?,
    ) : SplashReducerEvent

    /** 진입 절차를 다시 시작한다 */
    data object CheckStarted : SplashReducerEvent

    /** 연결 실패로 세션을 확인하지 못했다. */
    data object ConnectionFailed : SplashReducerEvent
}
