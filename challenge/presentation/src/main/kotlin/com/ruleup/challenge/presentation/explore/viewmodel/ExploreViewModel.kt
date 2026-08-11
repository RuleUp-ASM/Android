package com.ruleup.challenge.presentation.explore.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.observability.ChallengeCardSource
import com.ruleup.challenge.domain.observability.ChallengeEvents
import com.ruleup.challenge.domain.usecase.GetChallengeCategoriesUseCase
import com.ruleup.challenge.domain.usecase.GetTrendingChallengesUseCase
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
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
        private val observability: Observability,
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
                            // 서버는 Top 20 을 주지만 탐색 메인은 상위 5개만 노출한다(API 명세).
                            val shown = snapshot.items.take(TRENDING_MAIN_COUNT)
                            dispatch(
                                ExploreReducerEvent.TrendingLoaded(
                                    items = shown,
                                    calculatedAt = snapshot.calculatedAt,
                                ),
                            )
                            // 인기는 상위 N개가 한 화면에 함께 들어와 카드별로 쪼갤 이유가 없다 — 섹션 단위 1회.
                            if (shown.isNotEmpty()) {
                                observability.log(Channel.BUSINESS) {
                                    ChallengeEvents.trendingImpression(shown.map { it.challengeId })
                                }
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
                    retryOnce { getChallengeCategoriesUseCase() }
                        .onSuccess { dispatch(ExploreReducerEvent.CategoriesLoaded(it)) }
                        .onFailure { dispatch(ExploreReducerEvent.CategoriesFailed) }
                }
        }

        /** 1회 자동 재시도(프론트 스펙 4-4). 그 뒤부터는 사용자가 눌러야 다시 시도한다. */
        private suspend fun <T> retryOnce(block: suspend () -> T): Result<T> = runCatching { block() }.recoverCatching { block() }

        private fun openCategory(category: Category) {
            val count = currentState.categories.firstOrNull { it.category == category }?.activeGroupCount ?: 0
            observability.log(Channel.BUSINESS) { ChallengeEvents.categoryGridClick(category.value, count) }
            navigationHelper.navigateByRoute(ChallengeExploreListPage(category = category).toRoute())
        }

        /**
         * 인기 카드 클릭. **상세까지 같은 challenge_id 로 이어져야** 전환율이 계산되므로
         * source 를 함께 싣는다. 인기 섹션은 정렬 개념이 없어 sort 는 비운다.
         */
        private fun openChallenge(challengeId: String) {
            val position = currentState.trending.indexOfFirst { it.challengeId == challengeId }
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.challengeCardClick(
                    challengeId = challengeId,
                    position = position.coerceAtLeast(0),
                    source = ChallengeCardSource.TRENDING,
                    sort = null,
                )
            }
            navigationHelper.navigateByRoute(ChallengeDetailPage(challengeId).toRoute())
        }

        /**
         * 탐색 홈 진입은 전환율의 **분모**라 한 번만 보내야 한다. 인기·카테고리가 각각 끝날 때
         * 호출되므로 먼저 끝난 쪽에서 1회만 나가게 잠근다 — `has_trending` 은 인기 결과가 정해진
         * 뒤여야 의미가 있어 로드 완료 시점에 붙인다.
         */
        private fun logHomeViewOnce() {
            if (homeViewLogged) return
            homeViewLogged = true
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.exploreHomeView(hasTrending = !currentState.hideTrendingSection)
            }
        }

        private var homeViewLogged = false
    }
