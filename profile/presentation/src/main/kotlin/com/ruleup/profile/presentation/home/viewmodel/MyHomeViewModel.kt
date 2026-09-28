package com.ruleup.profile.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.notification.domain.navigation.NotificationCenterPage
import com.ruleup.notification.domain.navigation.NotificationSettingsPage
import com.ruleup.profile.domain.navigation.FriendInvitePage
import com.ruleup.profile.domain.navigation.MyAppealsPage
import com.ruleup.profile.domain.navigation.MyCalendarPage
import com.ruleup.profile.domain.navigation.MySettingsPage
import com.ruleup.profile.domain.navigation.MyStatsPage
import com.ruleup.profile.domain.navigation.MyTierPage
import com.ruleup.profile.domain.navigation.ProfileEditPage
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.report.domain.navigation.BlockListPage
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** 마이 홈 ViewModel. */
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
                MyHomeIntent.OpenTier -> navigationHelper.navigateByRoute(MyTierPage.toRoute())
                MyHomeIntent.OpenCalendar -> navigationHelper.navigateByRoute(MyCalendarPage().toRoute())
                MyHomeIntent.OpenAppeals -> navigationHelper.navigateByRoute(MyAppealsPage.toRoute())
                MyHomeIntent.OpenRanking -> pickChallenge(ChallengePickerTarget.RANKING)
                MyHomeIntent.OpenWatchers -> pickChallenge(ChallengePickerTarget.WATCHERS)
                is MyHomeIntent.SelectPickedChallenge -> {
                    val target = currentState.picker?.target ?: ChallengePickerTarget.RANKING
                    dispatch(MyHomeReducerEvent.PickerDismissed)
                    navigateToPicked(target, intent.challengeId)
                }

                MyHomeIntent.DismissChallengePicker -> dispatch(MyHomeReducerEvent.PickerDismissed)
                MyHomeIntent.OpenStats -> navigationHelper.navigateByRoute(MyStatsPage.toRoute())
                MyHomeIntent.OpenInvite -> navigationHelper.navigateByRoute(FriendInvitePage.toRoute())
                MyHomeIntent.OpenBlocks -> navigationHelper.navigateTo(BlockListPage)
                MyHomeIntent.OpenNotificationSettings -> navigationHelper.navigateTo(NotificationSettingsPage)
                MyHomeIntent.OpenNotificationCenter -> navigationHelper.navigateTo(NotificationCenterPage)

                MyHomeIntent.OpenSettings -> navigationHelper.navigateTo(MySettingsPage)
                MyHomeIntent.OpenHomeTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                MyHomeIntent.OpenChallengeTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))
                MyHomeIntent.OpenMyChallengesTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_LIST))
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

                is MyHomeReducerEvent.StatsLoaded -> state.copy(stats = event.stats)

                is MyHomeReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is MyHomeReducerEvent.LoadingPicker -> state.copy(isLoadingPicker = event.loading)

                is MyHomeReducerEvent.PickerShown -> state.copy(picker = event.picker)

                MyHomeReducerEvent.PickerDismissed -> state.copy(picker = null)
            }

        private fun load(force: Boolean) {
            // 첫 구독 시에만 스피너
            if (!force && currentState.home != null) return
            if (currentState.home == null) dispatch(MyHomeReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getHome() }
                    .onSuccess { dispatch(MyHomeReducerEvent.Loaded(it)) }
                    .onFailure {
                        if (currentState.home == null) {
                            dispatch(MyHomeReducerEvent.Failed(it.loadFailureMessage()))
                        }
                    }
            }
            // 성공률만 쓰는 부수 조회라 실패를 삼킨다
            viewModelScope.launch {
                runCatching { myPageRepository.getStats() }
                    .onSuccess { dispatch(MyHomeReducerEvent.StatsLoaded(it)) }
            }
        }

        /** 방을 먼저 고르는 메뉴들. */
        private fun pickChallenge(target: ChallengePickerTarget) {
            if (currentState.isLoadingPicker) return
            viewModelScope.launch {
                dispatch(MyHomeReducerEvent.LoadingPicker(true))
                runCatching { myPageRepository.getMyGroupChallenges() }
                    .onSuccess { challenges ->
                        when {
                            challenges.isEmpty() ->
                                emitEffect(MyHomeEffect.ShowMessage("참여 중인 그룹 챌린지가 없어요"))

                            challenges.size == 1 -> navigateToPicked(target, challenges.first().challengeId)

                            else ->
                                dispatch(
                                    MyHomeReducerEvent.PickerShown(
                                        ChallengePicker(target = target, challenges = challenges),
                                    ),
                                )
                        }
                    }.onFailure {
                        emitEffect(MyHomeEffect.ShowMessage(it.userFacingMessage("그룹 정보를 불러오지 못했어요")))
                    }
                dispatch(MyHomeReducerEvent.LoadingPicker(false))
            }
        }

        /** 고른 방으로 이동한다. */
        private fun navigateToPicked(
            target: ChallengePickerTarget,
            challengeId: String,
        ) {
            val path =
                when (target) {
                    ChallengePickerTarget.RANKING -> AppRoutes.CHALLENGE_RANKING
                    ChallengePickerTarget.WATCHERS -> AppRoutes.CHALLENGE_DETAIL
                }
            navigationHelper.navigateByRoute(NavRoute(path, mapOf("challengeId" to challengeId)))
        }
    }

// 네트워크 예외 메시지는 영문 원문(Unable to resolve host …)이라 화면에 그대로 내보내지 않는다.
private fun Throwable.loadFailureMessage(): String =
    if (this is IOException) "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요." else message ?: "마이 정보를 불러오지 못했어요"
