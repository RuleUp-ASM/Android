package com.ruleup.challenge.presentation.explore.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.observability.ChallengeCardSource
import com.ruleup.challenge.domain.observability.ChallengeEvents
import com.ruleup.challenge.domain.observability.ExploreListEntry
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
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
        private val exploreRepository: ExploreRepository,
        private val navigationHelper: NavigationHelper,
        private val observability: Observability,
    ) : MviViewModel<ExploreListIntent, ExploreListState, ExploreListReducerEvent, NoEffect>(
            ExploreListState.initial,
        ) {
        private var loaded = false

        // 스크롤 깊이 집계용. 첫 페이지가 0이고 이어붙일 때마다 1씩 오른다.
        private var pageIndex = 0

        // 세션 내 노출 중복 제거. 필터·정렬이 바뀌면 목록 자체가 달라지므로 비운다.
        private val impressed = mutableSetOf<String>()

        override fun onIntent(intent: ExploreListIntent) {
            when (intent) {
                is ExploreListIntent.Load -> load(intent.category, intent.sort)
                ExploreListIntent.LoadMore -> loadMore()
                is ExploreListIntent.ApplyFilter ->
                    fetchFirstPage(intent.filter, currentState.sort, LogAfterLoad.FilterApplied(intent.filter))

                is ExploreListIntent.SelectSort ->
                    fetchFirstPage(
                        currentState.filter,
                        intent.sort,
                        LogAfterLoad.SortChanged(from = currentState.sort, to = intent.sort),
                    )
                ExploreListIntent.ClearEligibleOnly ->
                    fetchFirstPage(currentState.filter.copy(eligibleOnly = false), currentState.sort)

                is ExploreListIntent.CardImpression -> logImpression(intent.challengeId)

                is ExploreListIntent.OpenChallenge -> openChallenge(intent.challengeId)

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
            val initialFilter = ExploreFilter(categories = setOfNotNull(prefilled))
            val initialSort = ExploreSort.fromValue(sort) ?: ExploreSort.default
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.exploreListView(
                    entry = if (prefilled != null) ExploreListEntry.CATEGORY else ExploreListEntry.ALL,
                    sort = initialSort,
                    filter = initialFilter,
                )
            }
            fetchFirstPage(filter = initialFilter, sort = initialSort)
        }

        /**
         * 카드 노출. **세션 내 중복은 여기서 막는다** — 스크롤로 같은 카드가 여러 번 들어오면
         * 노출 수가 부풀어 상세 진입률(클릭/노출)이 실제보다 낮게 나온다.
         */
        private fun logImpression(challengeId: String) {
            if (!impressed.add(challengeId)) return
            val index = currentState.items.indexOfFirst { it.challengeId == challengeId }
            val item = currentState.items.getOrNull(index) ?: return
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.challengeCardImpression(
                    challengeId = challengeId,
                    position = index,
                    sort = currentState.sort,
                    isFull = item.isFull,
                    eligible = item.eligible,
                    hasMetrics = item.hasMetrics,
                )
            }
        }

        /** 목록 카드 클릭. 노출→클릭→상세를 잇는 challenge_id 는 여기서 상세로 넘어간다. */
        private fun openChallenge(challengeId: String) {
            val position = currentState.items.indexOfFirst { it.challengeId == challengeId }
            observability.log(Channel.BUSINESS) {
                ChallengeEvents.challengeCardClick(
                    challengeId = challengeId,
                    position = position.coerceAtLeast(0),
                    source = ChallengeCardSource.LIST,
                    sort = currentState.sort,
                )
            }
            navigationHelper.navigateByRoute(ChallengeDetailPage(challengeId).toRoute())
        }

        /**
         * 첫 페이지 결과가 나온 뒤에야 보낼 수 있는 이벤트. `result_count` 를 실어야 해서
         * 인텐트 시점이 아니라 응답 시점에 발행한다.
         */
        private sealed interface LogAfterLoad {
            data class FilterApplied(
                val filter: ExploreFilter,
            ) : LogAfterLoad

            data class SortChanged(
                val from: ExploreSort,
                val to: ExploreSort,
            ) : LogAfterLoad
        }

        private fun fetchFirstPage(
            filter: ExploreFilter,
            sort: ExploreSort,
            logAfterLoad: LogAfterLoad? = null,
        ) {
            viewModelScope.launch {
                dispatch(ExploreListReducerEvent.Loading(filter = filter, sort = sort))
                impressed.clear()
                runCatching { exploreRepository.explore(filter = filter, sort = sort) }
                    .onSuccess { result ->
                        dispatch(
                            ExploreListReducerEvent.FirstPageLoaded(
                                items = result.items,
                                nextCursor = result.nextCursor,
                            ),
                        )
                        pageIndex = 0
                        logAfterLoad?.let { logResult(it, result.items.size) }
                        // 빈 결과는 filter_apply 와 **중복으로** 보낸다 — 분모가 달라 하나로 합칠 수 없다.
                        if (result.items.isEmpty()) {
                            observability.log(Channel.BUSINESS) {
                                ChallengeEvents.exploreEmptyResult(filter, sort)
                            }
                        }
                    }.onFailure { error -> recoverOrFail(error, filter, sort) }
            }
        }

        private fun logResult(
            log: LogAfterLoad,
            resultCount: Int,
        ) {
            when (log) {
                is LogAfterLoad.FilterApplied ->
                    observability.log(Channel.BUSINESS) {
                        ChallengeEvents.exploreFilterApply(log.filter, resultCount)
                    }

                is LogAfterLoad.SortChanged ->
                    observability.log(Channel.BUSINESS) {
                        ChallengeEvents.exploreSortChange(log.from, log.to, resultCount)
                    }
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
                    exploreRepository.explore(
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
                    pageIndex += 1
                    observability.log(Channel.BUSINESS) {
                        ChallengeEvents.exploreListLoadMore(pageIndex, currentState.sort)
                    }
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
