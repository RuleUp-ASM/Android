package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.UiState

data class SplashState(
    // 자동 로그인 판별 중 여부.
    val isChecking: Boolean = true,
    // 강제 업데이트 필요 여부.
    val forceUpdate: Boolean = false,
    // 안내 문구에 넣을 최소 지원 버전.
    val minAppVersion: String? = null,
    // 연결 실패로 세션을 확인하지 못했다.
    val connectionFailed: Boolean = false,
) : UiState {
    companion object {
        val initial = SplashState()
    }
}
