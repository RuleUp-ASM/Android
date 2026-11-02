package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.ui.mvi.UiState

data class LoginState(
    val isLoading: Boolean = false,
    // 심사 코드 시도 한도 초과 — 서버가 준 대기 시간 동안 다시 제출하지 못한다.
    val isReviewCoolingDown: Boolean = false,
) : UiState {
    companion object {
        val initial = LoginState()
    }
}
