package com.ruleup.profile.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.profile.domain.navigation.FriendInvitePage
import com.ruleup.profile.domain.navigation.MyCalendarPage
import com.ruleup.profile.domain.navigation.MyStatsPage
import com.ruleup.profile.domain.navigation.MyTemperaturePage
import com.ruleup.profile.domain.navigation.ProfileEditPage
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 마이 홈 ViewModel. GET /me/home 일괄 조회로 온도·카운트·프로필을 렌더링하고 메뉴 진입점을 제공한다.
 * 그룹 랭킹은 challengeId 단위(방 내부 스펙 재사용)라, 참여 중 그룹 챌린지를 골라 진입시킨다.
 */
@HiltViewModel
class MyHomeViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyHomeIntent, MyHomeState, MyHomeReducerEvent, MyHomeEffect>(
            MyHomeState.initial,
        ) {
        override fun onIntent(intent: MyHomeIntent) {
            when (intent) {
                MyHomeIntent.Load -> load(force = false)
                MyHomeIntent.Refresh -> load(force = true)
                MyHomeIntent.OpenProfileEdit -> navigationHelper.navigateByRoute(ProfileEditPage.toRoute())
                MyHomeIntent.OpenTemperature -> navigationHelper.navigateByRoute(MyTemperaturePage.toRoute())
                MyHomeIntent.OpenCalendar -> navigationHelper.navigateByRoute(MyCalendarPage.toRoute())
                MyHomeIntent.OpenRanking -> openRanking()
                is MyHomeIntent.SelectRankingChallenge -> {
                    dispatch(MyHomeReducerEvent.RankingPickerDismissed)
                    navigateToRanking(intent.challengeId)
                }

                MyHomeIntent.DismissRankingPicker -> dispatch(MyHomeReducerEvent.RankingPickerDismissed)
                MyHomeIntent.OpenStats -> navigationHelper.navigateByRoute(MyStatsPage.toRoute())
                MyHomeIntent.OpenInvite -> navigationHelper.navigateByRoute(FriendInvitePage.toRoute())
                MyHomeIntent.OpenSettings -> emitEffect(MyHomeEffect.ShowMessage("설정은 준비 중이에요"))
                MyHomeIntent.OpenHomeTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                MyHomeIntent.OpenChallengeTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))
            }
        }

        override fun reduce(
            state: MyHomeState,
            event: MyHomeReducerEvent,
        ): MyHomeState =
            when (event) {
                MyHomeReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyHomeReducerEvent.Loaded ->
                    state.copy(isLoading = false, home = event.home, errorMessage = null)

                is MyHomeReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is MyHomeReducerEvent.LoadingRanking -> state.copy(isLoadingRanking = event.loading)

                is MyHomeReducerEvent.RankingPickerShown -> state.copy(rankingPicker = event.challenges)

                MyHomeReducerEvent.RankingPickerDismissed -> state.copy(rankingPicker = null)
            }

        private fun load(force: Boolean) {
            // 첫 구독 시에만 스피너 — ON_RESUME 재조회는 기존 화면을 유지한 채 조용히 갱신한다.
            if (!force && currentState.home != null) return
            if (currentState.home == null) dispatch(MyHomeReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getHome() }
                    .onSuccess { dispatch(MyHomeReducerEvent.Loaded(it)) }
                    .onFailure {
                        if (currentState.home == null) {
                            dispatch(MyHomeReducerEvent.Failed(it.message ?: "마이 정보를 불러오지 못했어요"))
                        }
                    }
            }
        }

        // 그룹 챌린지 0개 = 안내, 1개 = 바로 랭킹, 2개+ = 선택 시트.
        private fun openRanking() {
            if (currentState.isLoadingRanking) return
            viewModelScope.launch {
                dispatch(MyHomeReducerEvent.LoadingRanking(true))
                runCatching { myPageRepository.getMyGroupChallenges() }
                    .onSuccess { challenges ->
                        when {
                            challenges.isEmpty() ->
                                emitEffect(MyHomeEffect.ShowMessage("참여 중인 그룹 챌린지가 없어요"))

                            challenges.size == 1 -> navigateToRanking(challenges.first().challengeId)

                            else -> dispatch(MyHomeReducerEvent.RankingPickerShown(challenges))
                        }
                    }.onFailure {
                        emitEffect(MyHomeEffect.ShowMessage(it.message ?: "그룹 정보를 불러오지 못했어요"))
                    }
                dispatch(MyHomeReducerEvent.LoadingRanking(false))
            }
        }

        // 랭킹 화면은 방 내부 스펙(챌린지 feature) 소유 — 공개 라우트 상수로 진입한다.
        private fun navigateToRanking(challengeId: String) {
            navigationHelper.navigateByRoute(
                NavRoute(AppRoutes.CHALLENGE_RANKING, mapOf("challengeId" to challengeId)),
            )
        }
    }
