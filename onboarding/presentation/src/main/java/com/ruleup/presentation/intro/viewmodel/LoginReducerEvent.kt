package com.ruleup.presentation.intro.viewmodel

import com.ruleup.ui.mvi.ReducerEvent

sealed interface LoginReducerEvent : ReducerEvent {
    data object Loaded : LoginReducerEvent

    data object LoginStarted : LoginReducerEvent

    data object LoginFinished : LoginReducerEvent
}
