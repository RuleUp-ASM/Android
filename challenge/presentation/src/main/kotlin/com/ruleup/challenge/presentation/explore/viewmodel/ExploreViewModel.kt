package com.ruleup.challenge.presentation.explore.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExploreListPage
import com.ruleup.challenge.domain.usecase.GetChallengeCategoriesUseCase
import com.ruleup.challenge.domain.usecase.GetTrendingChallengesUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.entity.user.InterestCategory
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// 탐색 메인에 노출할 실시간 인기 개수(서버는 Top 20 반환).
private const val TRENDING_MAIN_COUNT = 5

/**
 * 탐색 메인 ViewModel. 실시간 인기(서버 산정, 신선도 10분)와 카테고리별 챌린지 수를 병렬 조회한다.
 * 랭킹·집계는 서버 값을 그대로 노출하고, 클라이언트는 재계산하지 않는다.
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
                ExploreIntent.Load -> load()
                is ExploreIntent.OpenChallenge ->
                    navigationHelper.navigateByRoute(ChallengeDetailPage(intent.challengeId).toRoute())

                ExploreIntent.OpenTrendingAll ->
                    navigationHelper.navigateByRoute(ChallengeExploreListPage(sort = ExploreSort.TRENDING).toRoute())

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
                ExploreReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is ExploreReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        trending = event.trending,
                        categories = event.categories,
                        errorMessage = null,
                    )

                is ExploreReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            viewModelScope.launch {
                dispatch(ExploreReducerEvent.Loading)
                runCatching {
                    coroutineScope {
                        val trending = async { getTrendingChallengesUseCase() }
                        val categories = async { getChallengeCategoriesUseCase() }
                        trending.await() to categories.await()
                    }
                }.onSuccess { (trending, categories) ->
                    dispatch(
                        ExploreReducerEvent.Loaded(
                            // 서버는 Top 20 을 내려주지만 탐색 메인은 상위 5개만 노출한다(API 명세).
                            trending = trending.take(TRENDING_MAIN_COUNT),
                            categories = categories,
                        ),
                    )
                }.onFailure { dispatch(ExploreReducerEvent.Failed(it.message ?: "탐색 정보를 불러오지 못했어요")) }
            }
        }

        private fun openCategory(category: InterestCategory) {
            navigationHelper.navigateByRoute(ChallengeExploreListPage(category = category).toRoute())
        }
    }
