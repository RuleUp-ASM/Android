package com.ruleup.onboarding.presentation.walkthrough.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.intro.repository.WalkthroughRepository
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 첫 실행 소개 3장. */
@HiltViewModel
class WalkthroughViewModel
    @Inject
    constructor(
        private val walkthroughRepository: WalkthroughRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<WalkthroughIntent, WalkthroughState, WalkthroughReducerEvent, NoEffect>(WalkthroughState.initial) {
        override fun onIntent(intent: WalkthroughIntent) {
            when (intent) {
                WalkthroughIntent.Next ->
                    if (currentState.page.isLast) finish() else dispatch(WalkthroughReducerEvent.PageChanged(currentState.page.next()))

                WalkthroughIntent.Skip -> finish()
            }
        }

        override fun reduce(
            state: WalkthroughState,
            event: WalkthroughReducerEvent,
        ): WalkthroughState =
            when (event) {
                is WalkthroughReducerEvent.PageChanged -> state.copy(page = event.page)
            }

        private fun finish() {
            viewModelScope.launch {
                walkthroughRepository.markSeen()
                navigationHelper.navigateTo(LoginPage)
            }
        }
    }
