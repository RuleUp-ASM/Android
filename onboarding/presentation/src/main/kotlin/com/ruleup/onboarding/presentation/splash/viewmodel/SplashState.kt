package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.UiState

data class SplashState(
    // 자동 로그인 판별 중 여부. 판별이 끝나면 즉시 다른 화면으로 이동한다.
    val isChecking: Boolean = true,
    // 강제 업데이트 필요 여부. true 면 더 진행하지 않고 강제 업데이트 화면을 띄운다.
    val forceUpdate: Boolean = false,
    // 안내 문구에 넣을 최소 지원 버전. 없으면 화면이 일반 문구로 떨어진다.
    val minAppVersion: String? = null,
    // 연결 실패로 세션을 확인하지 못했다. **세션은 살아 있으므로 로그인 화면으로 보내지 않는다** —
    // 스플래시에 머문 채 다시 시도할 자리를 준다(ENV-03).
    val connectionFailed: Boolean = false,
) : UiState {
    companion object {
        val initial = SplashState()
    }
}
