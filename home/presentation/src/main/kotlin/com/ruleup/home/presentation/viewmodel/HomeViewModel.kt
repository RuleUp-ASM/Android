package com.ruleup.home.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.MyChallengesPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.time.ServiceDate
import com.ruleup.home.presentation.mergeHomeChallenges
import com.ruleup.notification.domain.navigation.NotificationCenterPage
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 홈 ViewModel. */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val verificationRepository: VerificationRepository,
        private val myChallengeStore: MyChallengeStore,
        private val notificationRepository: NotificationRepository,
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<HomeIntent, HomeState, HomeReducerEvent, NoEffect>(HomeState.initial) {
        // 진행 중 로드.
        private var loadJob: Job? = null

        override fun onIntent(intent: HomeIntent) {
            when (intent) {
                HomeIntent.Load -> {
                    load()
                }

                HomeIntent.CreateChallenge -> {
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_CREATE))
                }

                HomeIntent.OpenExplore -> {
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_EXPLORE))
                }

                HomeIntent.OpenMy -> {
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))
                }

                HomeIntent.OpenMyChallenges -> {
                    navigationHelper.navigateTo(MyChallengesPage)
                }

                HomeIntent.OpenNotifications -> {
                    navigationHelper.navigateTo(NotificationCenterPage)
                }

                is HomeIntent.OpenChallenge -> {
                    navigationHelper.navigateByRoute(ChallengeDetailPage(intent.challengeId).toRoute())
                }
            }
        }

        override fun reduce(
            state: HomeState,
            event: HomeReducerEvent,
        ): HomeState =
            when (event) {
                HomeReducerEvent.Loading -> {
                    state.copy(isLoading = true)
                }

                is HomeReducerEvent.Loaded -> {
                    state.copy(
                        isLoading = false,
                        challenges = event.challenges,
                    )
                }

                is HomeReducerEvent.UnreadChecked -> {
                    state.copy(hasUnreadNotifications = event.hasUnread)
                }

                is HomeReducerEvent.WeekLoaded -> {
                    state.copy(weekStatuses = event.statuses)
                }
            }

        /** 이번 주 날짜별 판정. 주가 두 달에 걸치면 두 달을 받는다. */
        private fun loadWeek() {
            viewModelScope.launch {
                val today = ServiceDate.today()
                val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                val week = (0L..6L).map { monday.plusDays(it).toString() }.toSet()
                val months = listOf(monday, monday.plusDays(6)).map { it.toString().take(7) }.distinct()
                val statuses =
                    months
                        .flatMap { month -> runCatching { myPageRepository.getCalendar(month).days }.getOrDefault(emptyList()) }
                        .filter { it.date in week }
                        .mapNotNull { day -> day.status?.let { day.date to it } }
                        .toMap()
                dispatch(HomeReducerEvent.WeekLoaded(statuses))
            }
        }

        /** 레드닷용 미읽음 확인. */
        private fun checkUnread() {
            viewModelScope.launch {
                runCatching { notificationRepository.getUnreadSummary() }
                    .onSuccess { dispatch(HomeReducerEvent.UnreadChecked(it.hasUnread)) }
            }
        }

        private fun load() {
            if (loadJob?.isActive == true) return
            checkUnread()
            loadWeek()
            loadJob =
                viewModelScope.launch {
                    // 데이터가 이미 있으면 스피너를 띄우지 않는다
                    if (currentState.challenges.isEmpty()) dispatch(HomeReducerEvent.Loading)
                    // 서로 독립인 두 조회라 병렬로 돌린다.
                    val (myChallenges, progress) =
                        coroutineScope {
                            // 홈은 진행 중만 보여 준다.
                            val challenges =
                                async {
                                    runCatching { challengeRepository.getMyChallenges().challenges }
                                        .getOrDefault(emptyList())
                                }
                            val progressSnapshot =
                                async { runCatching { verificationRepository.getProgress() }.getOrNull() }
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
