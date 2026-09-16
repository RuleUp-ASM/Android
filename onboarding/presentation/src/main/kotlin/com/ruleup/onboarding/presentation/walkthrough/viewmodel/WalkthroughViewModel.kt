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

/**
 * 첫 실행 소개 3장.
 *
 * 끝내는 길이 둘(마지막 장 CTA·건너뛰기)인데 **둘 다 같은 처리를 해야 한다** — 본 것으로 기록하고
 * 로그인으로 넘긴다. 기록을 건너뛰기 쪽에서 빠뜨리면 앱을 열 때마다 소개가 다시 뜬다.
 *
 * 기록이 실패해도 이동은 막지 않는다([WalkthroughRepository.markSeen] 이 예외를 삼킨다) —
 * 저장소 문제로 로그인 화면에 못 가는 것이 소개를 한 번 더 보는 것보다 나쁘다.
 */
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
