package com.ruleup.challenge.presentation.explore.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.usecase.ExploreChallengesUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.entity.user.InterestCategory
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 챌린지 둘러보기 ViewModel. 필터(AND)·정렬(7종)·커서 페이지네이션은 서버가 수행하고,
 * 화면은 (필터, 정렬) 이 바뀌면 첫 페이지부터, 스크롤 하단 근접 시 nextCursor 로 이어 붙인다.
 */
@HiltViewModel
class ExploreListViewModel
    @Inject
    constructor(
        private val exploreChallengesUseCase: ExploreChallengesUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ExploreListIntent, ExploreListState, ExploreListReducerEvent, NoEffect>(
            ExploreListState.initial,
        ) {
        private var loaded = false
        private var previewJob: Job? = null

        override fun onIntent(intent: ExploreListIntent) {
            when (intent) {
                is ExploreListIntent.Load -> load(intent.category, intent.sort)
                ExploreListIntent.LoadMore -> loadMore()
                is ExploreListIntent.ApplyFilter -> fetchFirstPage(intent.filter, currentState.sort)
                is ExploreListIntent.PreviewFilter -> previewFilter(intent.filter)
                is ExploreListIntent.SelectSort -> fetchFirstPage(currentState.filter, intent.sort)
                is ExploreListIntent.OpenChallenge ->
                    navigationHelper.navigateByRoute(ChallengeDetailPage(intent.challengeId).toRoute())

                ExploreListIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ExploreListState,
            event: ExploreListReducerEvent,
        ): ExploreListState =
            when (event) {
                is ExploreListReducerEvent.Loading ->
                    state.copy(
                        isLoading = true,
                        isLoadingMore = false,
                        filter = event.filter,
                        sort = event.sort,
                        errorMessage = null,
                    )

                is ExploreListReducerEvent.FirstPageLoaded ->
                    state.copy(
                        isLoading = false,
                        totalCount = event.totalCount,
                        challenges = event.challenges,
                        nextCursor = event.nextCursor,
                        errorMessage = null,
                        // 적용된 필터의 카운트가 곧 최신 미리보기 값
                        previewCount = event.totalCount,
                    )

                ExploreListReducerEvent.LoadingMore -> state.copy(isLoadingMore = true)

                is ExploreListReducerEvent.MorePageLoaded ->
                    state.copy(
                        isLoadingMore = false,
                        challenges = state.challenges + event.challenges,
                        nextCursor = event.nextCursor,
                    )

                is ExploreListReducerEvent.Failed ->
                    state.copy(isLoading = false, isLoadingMore = false, errorMessage = event.message)

                ExploreListReducerEvent.PreviewCounting -> state.copy(previewCount = null)

                is ExploreListReducerEvent.PreviewCounted -> state.copy(previewCount = event.count)
            }

        private fun load(
            category: String?,
            sort: String?,
        ) {
            if (loaded) return
            loaded = true
            fetchFirstPage(
                filter = ExploreFilter(category = InterestCategory.fromValue(category.orEmpty())),
                sort = ExploreSort.fromValue(sort) ?: ExploreSort.PARTICIPANTS,
            )
        }

        private fun fetchFirstPage(
            filter: ExploreFilter,
            sort: ExploreSort,
        ) {
            viewModelScope.launch {
                dispatch(ExploreListReducerEvent.Loading(filter = filter, sort = sort))
                runCatching { exploreChallengesUseCase(filter = filter, sort = sort) }
                    .onSuccess { result ->
                        dispatch(
                            ExploreListReducerEvent.FirstPageLoaded(
                                totalCount = result.totalCount,
                                challenges = result.challenges,
                                nextCursor = result.nextCursor,
                            ),
                        )
                    }.onFailure { dispatch(ExploreListReducerEvent.Failed(it.message ?: "챌린지를 불러오지 못했어요")) }
            }
        }

        private fun loadMore() {
            val cursor = currentState.nextCursor
            if (cursor == null || !currentState.canLoadMore) return
            viewModelScope.launch {
                dispatch(ExploreListReducerEvent.LoadingMore)
                runCatching {
                    exploreChallengesUseCase(
                        filter = currentState.filter,
                        sort = currentState.sort,
                        cursor = cursor,
                    )
                }.onSuccess { result ->
                    dispatch(
                        ExploreListReducerEvent.MorePageLoaded(
                            challenges = result.challenges,
                            nextCursor = result.nextCursor,
                        ),
                    )
                }.onFailure { dispatch(ExploreListReducerEvent.Failed(it.message ?: "챌린지를 불러오지 못했어요")) }
            }
        }

        // 시트에서 조건을 바꿀 때마다 최소 페이지(size=1)로 totalCount 만 미리 집계한다. 연타 시 직전 요청은 취소.
        private fun previewFilter(filter: ExploreFilter) {
            previewJob?.cancel()
            previewJob =
                viewModelScope.launch {
                    dispatch(ExploreListReducerEvent.PreviewCounting)
                    runCatching { exploreChallengesUseCase(filter = filter, sort = currentState.sort, size = 1) }
                        .onSuccess { dispatch(ExploreListReducerEvent.PreviewCounted(it.totalCount)) }
                        .onFailure { /* 미리보기 실패는 무시 — 버튼엔 카운트 없이 "결과 보기"만 노출 */ }
                }
        }
    }
