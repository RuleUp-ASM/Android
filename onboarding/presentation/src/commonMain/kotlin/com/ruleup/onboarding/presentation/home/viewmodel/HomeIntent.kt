package com.ruleup.onboarding.presentation.home.viewmodel

import com.ruleup.ui.mvi.MviIntent

sealed interface HomeIntent : MviIntent {
    /** 챌린지 생성. 아직 실제 동작은 없다(화면 미정). */
    data object CreateChallenge : HomeIntent

    data object Logout : HomeIntent
}
