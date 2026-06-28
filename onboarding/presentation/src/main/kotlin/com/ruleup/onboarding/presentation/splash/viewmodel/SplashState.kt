package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.UiState

data class SplashState(
    // 자동 로그인 판별 중 여부. 판별이 끝나면 즉시 다른 화면으로 이동한다.
    val isChecking: Boolean = true,
    // 강제 업데이트 필요 여부. true 면 더 진행하지 않고 강제 업데이트 화면을 띄운다.
    val forceUpdate: Boolean = false,
    // 강제 업데이트 화면에 노출할 안내 메시지(서버 devTestMsg, 없으면 null → 기본 문구).
    val updateMessage: String? = null,
) : UiState {
    companion object {
        val initial = SplashState()
    }
}
