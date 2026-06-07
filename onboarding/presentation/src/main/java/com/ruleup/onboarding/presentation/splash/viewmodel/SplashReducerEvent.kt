package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.ui.mvi.ReducerEvent

sealed interface SplashReducerEvent : ReducerEvent {
    data object CheckFinished : SplashReducerEvent
}
