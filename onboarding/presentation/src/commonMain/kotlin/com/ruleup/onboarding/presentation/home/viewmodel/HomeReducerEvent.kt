package com.ruleup.onboarding.presentation.home.viewmodel

import com.ruleup.ui.mvi.ReducerEvent

sealed interface HomeReducerEvent : ReducerEvent {
    data object LogoutStarted : HomeReducerEvent
}
