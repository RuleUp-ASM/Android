package com.ruleup.presentation.profile.vm

import com.ruleup.core.ui.mvi.MviEffect

sealed interface ProfileEffect : MviEffect {
    /** 마지막 단계 완료 → 온보딩 종료(홈으로). 단계 간 이동은 상태(step)로 처리하므로 effect 가 아니다. */
    data object NavigateToHome : ProfileEffect

    data class ShowError(
        val message: String,
    ) : ProfileEffect
}
