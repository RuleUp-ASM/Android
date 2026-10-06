package com.ruleup.home.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.navigation.MyChallengesPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.time.ServiceDate
import com.ruleup.home.presentation.HomeChallengeUi
import com.ruleup.home.presentation.heroCandidates
import com.ruleup.home.presentation.mergeHomeChallenges
import com.ruleup.home.presentation.pickStarters
import com.ruleup.notification.domain.navigation.NotificationCenterPage
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        private val exploreRepository: ExploreRepository,
        private val profileRepository: ProfileRepository,
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

                is HomeIntent.OpenCategory -> {
                    navigationHelper.navigateByRoute(ChallengeExploreListPage(category = intent.category).toRoute())
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

                is HomeReducerEvent.NicknameLoaded -> {
                    state.copy(nickname = event.nickname)
                }

                is HomeReducerEvent.CheckableLoaded -> {
                    state.copy(manualCheckable = state.manualCheckable + event.manualCheckable)
                }

                is HomeReducerEvent.StartersLoaded -> {
                    state.copy(starters = event.starters, interests = event.interests)
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

        /** 인사말 닉네임. 홈의 부수 정보라 실패해도 조용히 넘긴다. */
        private fun loadNickname() {
            if (currentState.nickname != null) return
            viewModelScope.launch {
                runCatching { myPageRepository.getHome().nickname }
                    .onSuccess { dispatch(HomeReducerEvent.NicknameLoaded(it)) }
            }
        }

        /**
         * 「오늘 해 볼까요?」 후보의 인증 방식을 상세로 확인한다. 목록 응답엔 인증 방식이 없다.
         * 인증 방식은 바뀌지 않으니 한 번 받은 챌린지는 다시 묻지 않는다.
         */
        private suspend fun loadCheckable(cards: List<HomeChallengeUi>) {
            val unknown = heroCandidates(cards).map { it.challengeId }.filter { it !in currentState.manualCheckable }
            if (unknown.isEmpty()) return
            val found =
                coroutineScope {
                    unknown
                        .map { id -> async { runCatching { id to challengeRepository.getChallenge(id).manualCheckable }.getOrNull() } }
                        .awaitAll()
                }.filterNotNull().toMap()
            if (found.isNotEmpty()) dispatch(HomeReducerEvent.CheckableLoaded(found))
        }

        /** 챌린지가 하나도 없는 사람에게 첫 챌린지를 고른다. 한 번 받으면 다시 부르지 않는다. */
        private suspend fun loadStarters() {
            if (currentState.starters.isNotEmpty()) return
            val (trending, interests) =
                coroutineScope {
                    val trending = async { runCatching { exploreRepository.getTrending().items }.getOrDefault(emptyList()) }
                    // 관심 분야를 못 받으면 인기 순서 그대로 보여 준다.
                    val interests = async { runCatching { profileRepository.getProfile().interestCategories }.getOrDefault(emptyList()) }
                    trending.await() to interests.await()
                }
            dispatch(HomeReducerEvent.StartersLoaded(pickStarters(trending, interests), interests))
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
            loadNickname()
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
                    val cards = mergeHomeChallenges(myChallenges, progress, myChallengeStore.all())
                    dispatch(HomeReducerEvent.Loaded(cards))
                    if (cards.isEmpty()) loadStarters() else loadCheckable(cards)
                }
        }
    }
