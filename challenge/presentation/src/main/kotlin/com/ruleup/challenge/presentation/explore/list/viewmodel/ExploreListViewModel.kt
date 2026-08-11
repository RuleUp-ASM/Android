package com.ruleup.challenge.presentation.explore.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.usecase.ExploreChallengesUseCase
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 챌린지 둘러보기 ViewModel.
 *
 * 필터(AND)·정렬(6종)·커서 페이지네이션은 서버가 수행하고, 화면은 (필터, 정렬) 이 바뀌면 첫 페이지부터
 * 다시, 스크롤 하단 근접 시 `nextCursor` 로 이어 붙인다. 진행 중인 요청이 있으면 중복 호출을 막는다.
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

        override fun onIntent(intent: ExploreListIntent) {
            when (intent) {
                is ExploreListIntent.Load -> load(intent.category, intent.sort)
                ExploreListIntent.LoadMore -> loadMore()
                is ExploreListIntent.ApplyFilter -> fetchFirstPage(intent.filter, currentState.sort)
                is ExploreListIntent.SelectSort -> fetchFirstPage(currentState.filter, intent.sort)
                ExploreListIntent.ClearEligibleOnly ->
                    fetchFirstPage(currentState.filter.copy(eligibleOnly = false), currentState.sort)

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
                        loadMoreFailed = false,
                        filter = event.filter,
                        sort = event.sort,
                        errorMessage = null,
                    )

                is ExploreListReducerEvent.FirstPageLoaded ->
                    state.copy(
                        isLoading = false,
                        items = event.items,
                        nextCursor = event.nextCursor,
                        errorMessage = null,
                    )

                ExploreListReducerEvent.LoadingMore -> state.copy(isLoadingMore = true, loadMoreFailed = false)

                is ExploreListReducerEvent.MorePageLoaded ->
                    state.copy(
                        isLoadingMore = false,
                        items = state.items + event.items,
                        nextCursor = event.nextCursor,
                    )

                // 다음 페이지 실패는 기존 목록을 지우지 않는다 — 하단에서만 재시도한다.
                ExploreListReducerEvent.LoadMoreFailed ->
                    state.copy(isLoadingMore = false, loadMoreFailed = true)

                is ExploreListReducerEvent.Failed ->
                    state.copy(isLoading = false, isLoadingMore = false, errorMessage = event.message)
            }

        private fun load(
            category: String?,
            sort: String?,
        ) {
            if (loaded) return
            loaded = true
            // 카테고리 타일로 들어오면 그 카테고리를 필터에 프리필하고, 화면은 칩으로 해제할 수 있게 한다.
            val prefilled = Category.fromValue(category.orEmpty())
            fetchFirstPage(
                filter = ExploreFilter(categories = setOfNotNull(prefilled)),
                sort = ExploreSort.fromValue(sort) ?: ExploreSort.default,
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
                                items = result.items,
                                nextCursor = result.nextCursor,
                            ),
                        )
                    }.onFailure { error -> recoverOrFail(error, filter, sort) }
            }
        }

        /**
         * 서버가 조건을 거절하면 **사용자에게 되묻지 않고 스스로 고쳐 다시 조회한다**.
         * 정렬 오류는 기본 정렬로, 필터 오류는 필터 초기화로, 커서 오류는 첫 페이지부터.
         */
        private fun recoverOrFail(
            error: Throwable,
            filter: ExploreFilter,
            sort: ExploreSort,
        ) {
            when (error) {
                is InvalidSortTypeException ->
                    if (sort != ExploreSort.default) fetchFirstPage(filter, ExploreSort.default) else fail(error)

                is InvalidFilterValueException ->
                    if (filter != ExploreFilter.none) fetchFirstPage(ExploreFilter.none, sort) else fail(error)

                is CursorInvalidException -> fetchFirstPage(filter, sort)

                else -> fail(error)
            }
        }

        private fun fail(error: Throwable) {
            dispatch(ExploreListReducerEvent.Failed(error.message ?: "챌린지를 불러오지 못했어요"))
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
                            items = result.items,
                            nextCursor = result.nextCursor,
                        ),
                    )
                }.onFailure { error ->
                    // 커서가 상해 있으면 조용히 첫 페이지부터 다시 받는다 — 사용자가 인지할 필요가 없다.
                    if (error is CursorInvalidException) {
                        fetchFirstPage(currentState.filter, currentState.sort)
                    } else {
                        dispatch(ExploreListReducerEvent.LoadMoreFailed)
                    }
                }
            }
        }
    }
