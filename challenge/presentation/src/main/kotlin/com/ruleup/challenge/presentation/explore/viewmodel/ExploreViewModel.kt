package com.ruleup.challenge.presentation.explore.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.logging.ChallengeCardSource
import com.ruleup.challenge.domain.logging.ChallengeEvents
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.navigation.MyChallengesPage
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.logging.domain.BizLogger
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

// 탐색 메인에 노출할 실시간 인기 개수(서버는 Top 20 반환).
private const val TRENDING_MAIN_COUNT = 5

/** 탐색 메인 ViewModel. */
@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
        private val navigationHelper: NavigationHelper,
        private val bizLogger: BizLogger,
    ) : MviViewModel<ExploreIntent, ExploreState, ExploreReducerEvent, NoEffect>(
            ExploreState.initial,
        ) {
        override fun onIntent(intent: ExploreIntent) {
            when (intent) {
                ExploreIntent.Load -> {
                    loadTrending()
                    loadCategories()
                }

                ExploreIntent.RetryTrending -> loadTrending()
                ExploreIntent.RetryCategories -> loadCategories()

                is ExploreIntent.OpenChallenge -> openChallenge(intent.challengeId)

                ExploreIntent.OpenTrendingAll ->
                    navigationHelper.navigateByRoute(ChallengeExploreListPage(sort = ExploreSort.default).toRoute())

                ExploreIntent.OpenCategoryAll ->
                    navigationHelper.navigateByRoute(ChallengeExploreListPage().toRoute())

                is ExploreIntent.OpenCategory -> openCategory(intent.category)
                ExploreIntent.OpenHome -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                ExploreIntent.OpenMy -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))

                ExploreIntent.OpenMyChallenges -> navigationHelper.navigateTo(MyChallengesPage)

                ExploreIntent.CreateChallenge ->
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.CHALLENGE_CREATE))
            }
        }

        override fun reduce(
            state: ExploreState,
            event: ExploreReducerEvent,
        ): ExploreState =
            when (event) {
                ExploreReducerEvent.TrendingLoading ->
                    state.copy(isTrendingLoading = true, trendingFailed = false)

                is ExploreReducerEvent.TrendingLoaded ->
                    state.copy(
                        isTrendingLoading = false,
                        trending = event.items,
                        calculatedAt = event.calculatedAt,
                        trendingFailed = false,
                    )

                ExploreReducerEvent.TrendingFailed ->
                    state.copy(isTrendingLoading = false, trendingFailed = true)

                ExploreReducerEvent.CategoriesLoading ->
                    state.copy(isCategoriesLoading = true, categoriesFailed = false)

                is ExploreReducerEvent.CategoriesLoaded ->
                    state.copy(
                        isCategoriesLoading = false,
                        categories = event.categories,
                        categoriesFailed = false,
                    )

                ExploreReducerEvent.CategoriesFailed ->
                    state.copy(isCategoriesLoading = false, categoriesFailed = true)
            }

        /** 진행 중인 요청과 겹치지 않게만 막는다 */
        private var trendingJob: Job? = null
        private var categoriesJob: Job? = null

        private fun loadTrending() {
            if (trendingJob?.isActive == true) return
            trendingJob =
                viewModelScope.launch {
                    dispatch(ExploreReducerEvent.TrendingLoading)
                    retryOnce { exploreRepository.getTrending() }
                        .onSuccess { snapshot ->
                            val shown = snapshot.items.take(TRENDING_MAIN_COUNT)
                            dispatch(
                                ExploreReducerEvent.TrendingLoaded(
                                    items = shown,
                                    calculatedAt = snapshot.calculatedAt,
                                ),
                            )
                            // 인기는 상위 N개가 한 화면에 함께 들어와 카드별로 쪼갤 이유가 없다
                            if (shown.isNotEmpty()) {
                                bizLogger.record(ChallengeEvents.trendingImpression(shown.map { it.challengeId }))
                            }
                            logHomeViewOnce()
                        }.onFailure {
                            dispatch(ExploreReducerEvent.TrendingFailed)
                            logHomeViewOnce()
                        }
                }
        }

        private fun loadCategories() {
            if (categoriesJob?.isActive == true) return
            categoriesJob =
                viewModelScope.launch {
                    dispatch(ExploreReducerEvent.CategoriesLoading)
                    retryOnce { exploreRepository.getCategories() }
                        .onSuccess { dispatch(ExploreReducerEvent.CategoriesLoaded(it)) }
                        .onFailure { dispatch(ExploreReducerEvent.CategoriesFailed) }
                }
        }

        /** 1회 자동 재시도. */
        private suspend fun <T> retryOnce(block: suspend () -> T): Result<T> = runCatching { block() }.recoverCatching { block() }

        private fun openCategory(category: Category) {
            val count = currentState.categories.firstOrNull { it.category == category }?.activeGroupCount ?: 0
            bizLogger.record(ChallengeEvents.categoryGridClick(category.value, count))
            navigationHelper.navigateByRoute(ChallengeExploreListPage(category = category).toRoute())
        }

        /** 인기 카드 클릭. */
        private fun openChallenge(challengeId: String) {
            val position = currentState.trending.indexOfFirst { it.challengeId == challengeId }
            bizLogger.record(
                ChallengeEvents.challengeCardClick(
                    challengeId = challengeId,
                    position = position.coerceAtLeast(0),
                    source = ChallengeCardSource.TRENDING,
                    sort = null,
                ),
            )
            navigationHelper.navigateByRoute(ChallengeDetailPage(challengeId).toRoute())
        }

        /** 탐색 홈 진입은 전환율의 분모라 한 번만 보내야 한다. */
        private fun logHomeViewOnce() {
            if (homeViewLogged) return
            homeViewLogged = true
            bizLogger.record(ChallengeEvents.exploreHomeView(hasTrending = !currentState.hideTrendingSection))
        }

        private var homeViewLogged = false
    }
