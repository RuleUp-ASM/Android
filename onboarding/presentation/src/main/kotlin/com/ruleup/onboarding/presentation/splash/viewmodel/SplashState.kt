package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.UiState

data class SplashState(
    // 자동 로그인 판별 중 여부. 판별이 끝나면 즉시 다른 화면으로 이동한다.
    val isChecking: Boolean = true,
    // 강제 업데이트 필요 여부. true 면 더 진행하지 않고 강제 업데이트 화면을 띄운다.
    val forceUpdate: Boolean = false,
    // 안내 문구에 넣을 최소 지원 버전. 없으면 화면이 일반 문구로 떨어진다.
    val minAppVersion: String? = null,
) : UiState {
    companion object {
        val initial = SplashState()
    }
}
