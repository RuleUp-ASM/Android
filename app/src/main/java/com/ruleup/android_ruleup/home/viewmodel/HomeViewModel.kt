package com.ruleup.android_ruleup.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.android_ruleup.home.mergeHomeChallenges
import com.ruleup.challenge.domain.ChallengeDetailPage
import com.ruleup.challenge.domain.MyChallengeStore
import com.ruleup.challenge.domain.usecase.GetMyChallengesUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.usecase.ObserveProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 ViewModel. 진입 시 서버 "내 챌린지 목록"(GET /challenges)을 기준으로, 진행률
 * (verification/progress)과 로컬 "내 챌린지"를 병합해 카드로 노출한다.
 * 목록·진행률 조회가 실패해도(아직 챌린지 없음 등) 나머지 소스만으로 렌더되도록 실패를 흡수한다.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getMyChallengesUseCase: GetMyChallengesUseCase,
        private val observeProgressUseCase: ObserveProgressUseCase,
        private val myChallengeStore: MyChallengeStore,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<HomeIntent, HomeState, HomeReducerEvent, NoEffect>(HomeState.initial) {
        override fun onIntent(intent: HomeIntent) {
            when (intent) {
                HomeIntent.Load -> load()
                HomeIntent.CreateChallenge ->
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_CREATE))

                is HomeIntent.OpenChallenge ->
                    navigationHelper.navigateByRoute(ChallengeDetailPage(intent.challengeId).toRoute())

                is HomeIntent.SelectFilter ->
                    dispatch(HomeReducerEvent.FilterSelected(intent.filter))
            }
        }

        override fun reduce(
            state: HomeState,
            event: HomeReducerEvent,
        ): HomeState =
            when (event) {
                HomeReducerEvent.Loading -> state.copy(isLoading = true)
                is HomeReducerEvent.Loaded -> state.copy(isLoading = false, challenges = event.challenges)
                is HomeReducerEvent.FilterSelected -> state.copy(filter = event.filter)
            }

        private fun load() {
            viewModelScope.launch {
                dispatch(HomeReducerEvent.Loading)
                val myChallenges = runCatching { getMyChallengesUseCase() }.getOrDefault(emptyList())
                val progress = runCatching { observeProgressUseCase() }.getOrNull()
                dispatch(
                    HomeReducerEvent.Loaded(
                        mergeHomeChallenges(myChallenges, progress, myChallengeStore.all()),
                    ),
                )
            }
        }
    }
