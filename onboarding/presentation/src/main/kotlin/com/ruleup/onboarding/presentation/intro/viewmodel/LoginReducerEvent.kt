package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.ui.mvi.ReducerEvent

sealed interface LoginReducerEvent : ReducerEvent {
    data object LoginStarted : LoginReducerEvent

    data object LoginFinished : LoginReducerEvent

    data object ReviewCooldownStarted : LoginReducerEvent

    data object ReviewCooldownEnded : LoginReducerEvent
}
