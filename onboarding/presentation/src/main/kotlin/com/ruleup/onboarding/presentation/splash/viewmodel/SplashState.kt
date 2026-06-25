package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.UiState

data class SplashState(
    // 자동 로그인 판별 중 여부. 판별이 끝나면 즉시 다른 화면으로 이동한다.
    val isChecking: Boolean = true,
) : UiState {
    companion object {
        val initial = SplashState()
    }
}
