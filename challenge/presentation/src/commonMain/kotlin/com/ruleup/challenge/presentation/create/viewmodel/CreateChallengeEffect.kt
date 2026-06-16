package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.ui.mvi.MviEffect

sealed interface CreateChallengeEffect : MviEffect {
    data class ShowError(
        val message: String,
    ) : CreateChallengeEffect

    /** AUTO 인증에 필요한 권한 토큰을 OS 다이얼로그로 요청하도록 화면에 지시한다. */
    data class RequestPermissions(
        val tokens: List<String>,
    ) : CreateChallengeEffect
}
