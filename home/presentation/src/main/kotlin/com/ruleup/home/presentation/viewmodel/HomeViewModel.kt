package com.ruleup.home.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.home.presentation.mergeHomeChallenges
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        private val challengeRepository: ChallengeRepository,
        private val verificationRepository: VerificationRepository,
        private val myChallengeStore: MyChallengeStore,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<HomeIntent, HomeState, HomeReducerEvent, NoEffect>(HomeState.initial) {
        // 진행 중 로드. 홈 재진입(LaunchedEffect 재발화)마다 중복 요청을 막는다.
        private var loadJob: Job? = null

        override fun onIntent(intent: HomeIntent) {
            when (intent) {
                HomeIntent.Load -> load()
                HomeIntent.CreateChallenge ->
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_CREATE))

                HomeIntent.OpenExplore ->
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))

                HomeIntent.OpenMy ->
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))

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
            // 이미 로드 중이면 중복 실행하지 않는다(재진입 시 요청 폭주 방지).
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    // 첫 로드만 로딩 스피너를 띄우고, 데이터가 이미 있으면 깜빡임 없이 백그라운드 새로고침한다.
                    if (currentState.challenges.isEmpty()) dispatch(HomeReducerEvent.Loading)
                    // 서로 독립인 두 조회를 병렬로 실행해 첫 렌더 지연을 줄인다(각 실패는 흡수).
                    val (myChallenges, progress) =
                        coroutineScope {
                            val challenges = async { runCatching { challengeRepository.getMyChallenges() }.getOrDefault(emptyList()) }
                            val progressSnapshot = async { runCatching { verificationRepository.getProgress() }.getOrNull() }
                            challenges.await() to progressSnapshot.await()
                        }
                    dispatch(
                        HomeReducerEvent.Loaded(
                            mergeHomeChallenges(myChallenges, progress, myChallengeStore.all()),
                        ),
                    )
                }
        }
    }
