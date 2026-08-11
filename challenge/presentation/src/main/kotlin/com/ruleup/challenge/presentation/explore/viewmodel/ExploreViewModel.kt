package com.ruleup.challenge.presentation.explore.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.usecase.GetChallengeCategoriesUseCase
import com.ruleup.challenge.domain.usecase.GetTrendingChallengesUseCase
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

// 탐색 메인에 노출할 실시간 인기 개수(서버는 Top 20 반환).
private const val TRENDING_MAIN_COUNT = 5

/**
 * 탐색 메인 ViewModel.
 *
 * 인기와 카테고리를 **독립된 코루틴으로 병렬 조회**한다 — 한쪽 실패가 다른 쪽을 막지 않아야 하므로
 * 하나의 `runCatching` 으로 묶지 않는다. 각각 1회 자동 재시도 후에도 실패하면 그 영역만 재시도 UI 로
 * 떨어진다. 랭킹·집계는 서버 값을 그대로 노출하고 클라이언트가 재계산하지 않는다.
 */
@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        private val getTrendingChallengesUseCase: GetTrendingChallengesUseCase,
        private val getChallengeCategoriesUseCase: GetChallengeCategoriesUseCase,
        private val navigationHelper: NavigationHelper,
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

                is ExploreIntent.OpenChallenge ->
                    navigationHelper.navigateByRoute(ChallengeDetailPage(intent.challengeId).toRoute())

                ExploreIntent.OpenTrendingAll ->
                    navigationHelper.navigateByRoute(ChallengeExploreListPage(sort = ExploreSort.default).toRoute())

                ExploreIntent.OpenCategoryAll ->
                    navigationHelper.navigateByRoute(ChallengeExploreListPage().toRoute())

                is ExploreIntent.OpenCategory -> openCategory(intent.category)
                ExploreIntent.OpenHome -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                ExploreIntent.OpenMy -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))
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

        /** 진행 중인 요청과 겹치지 않게만 막는다 — 화면 재진입·재시도는 그대로 통과시킨다. */
        private var trendingJob: Job? = null
        private var categoriesJob: Job? = null

        private fun loadTrending() {
            if (trendingJob?.isActive == true) return
            trendingJob =
                viewModelScope.launch {
                    dispatch(ExploreReducerEvent.TrendingLoading)
                    retryOnce { getTrendingChallengesUseCase() }
                        .onSuccess { snapshot ->
                            dispatch(
                                ExploreReducerEvent.TrendingLoaded(
                                    // 서버는 Top 20 을 주지만 탐색 메인은 상위 5개만 노출한다(API 명세).
                                    items = snapshot.items.take(TRENDING_MAIN_COUNT),
                                    calculatedAt = snapshot.calculatedAt,
                                ),
                            )
                        }.onFailure { dispatch(ExploreReducerEvent.TrendingFailed) }
                }
        }

        private fun loadCategories() {
            if (categoriesJob?.isActive == true) return
            categoriesJob =
                viewModelScope.launch {
                    dispatch(ExploreReducerEvent.CategoriesLoading)
                    retryOnce { getChallengeCategoriesUseCase() }
                        .onSuccess { dispatch(ExploreReducerEvent.CategoriesLoaded(it)) }
                        .onFailure { dispatch(ExploreReducerEvent.CategoriesFailed) }
                }
        }

        /** 1회 자동 재시도(프론트 스펙 4-4). 그 뒤부터는 사용자가 눌러야 다시 시도한다. */
        private suspend fun <T> retryOnce(block: suspend () -> T): Result<T> = runCatching { block() }.recoverCatching { block() }

        private fun openCategory(category: Category) {
            navigationHelper.navigateByRoute(ChallengeExploreListPage(category = category).toRoute())
        }
    }
