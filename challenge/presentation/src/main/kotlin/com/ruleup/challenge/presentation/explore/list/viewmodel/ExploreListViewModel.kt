package com.ruleup.challenge.presentation.explore.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.CursorInvalidException
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.InvalidFilterValueException
import com.ruleup.challenge.domain.entity.InvalidSortTypeException
import com.ruleup.challenge.domain.logging.ChallengeCardSource
import com.ruleup.challenge.domain.logging.ChallengeEvents
import com.ruleup.challenge.domain.logging.ExploreListEntry
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.logging.domain.BizLogger
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** 챌린지 둘러보기 ViewModel. */
@HiltViewModel
class ExploreListViewModel
    @Inject
    constructor(
        private val exploreRepository: ExploreRepository,
        private val navigationHelper: NavigationHelper,
        private val bizLogger: BizLogger,
    ) : MviViewModel<ExploreListIntent, ExploreListState, ExploreListReducerEvent, NoEffect>(
            ExploreListState.initial,
        ) {
        private var loaded = false

        // 스크롤 깊이 집계용.
        private var pageIndex = 0

        // 세션 내 노출 중복 제거.
        private val impressed = mutableSetOf<String>()

        override fun onIntent(intent: ExploreListIntent) {
            when (intent) {
                is ExploreListIntent.Load -> load(intent.category, intent.sort)
                ExploreListIntent.LoadMore -> loadMore()
                ExploreListIntent.Refresh ->
                    if (!currentState.isLoading && !currentState.isRefreshing) {
                        fetchFirstPage(currentState.filter, currentState.sort, refreshing = true)
                    }
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
                        isRefreshing = false,
                        items = event.items,
                        nextCursor = event.nextCursor,
                        errorMessage = null,
                    )

                ExploreListReducerEvent.LoadingMore -> state.copy(isLoadingMore = true, loadMoreFailed = false)

                ExploreListReducerEvent.Refreshing ->
                    state.copy(isRefreshing = true, isLoadingMore = false, loadMoreFailed = false, errorMessage = null)

                is ExploreListReducerEvent.MorePageLoaded ->
                    state.copy(
                        isLoadingMore = false,
                        items = state.items + event.items,
                        nextCursor = event.nextCursor,
                    )

                // 다음 페이지 실패는 기존 목록을 지우지 않는다
                ExploreListReducerEvent.LoadMoreFailed ->
                    state.copy(isLoadingMore = false, loadMoreFailed = true)

                is ExploreListReducerEvent.Failed ->
                    state.copy(isLoading = false, isLoadingMore = false, isRefreshing = false, errorMessage = event.message)
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
            bizLogger.record(
                ChallengeEvents.exploreListView(
                    entry = if (prefilled != null) ExploreListEntry.CATEGORY else ExploreListEntry.ALL,
                    sort = initialSort,
                    filter = initialFilter,
                ),
            )
            fetchFirstPage(filter = initialFilter, sort = initialSort)
        }

        /** 카드 노출. */
        private fun logImpression(challengeId: String) {
            if (!impressed.add(challengeId)) return
            val index = currentState.items.indexOfFirst { it.challengeId == challengeId }
            val item = currentState.items.getOrNull(index) ?: return
            bizLogger.record(
                ChallengeEvents.challengeCardImpression(
                    challengeId = challengeId,
                    position = index,
                    sort = currentState.sort,
                    isFull = item.isFull,
                    eligible = item.eligible,
                    hasMetrics = item.hasMetrics,
                ),
            )
        }

        /** 목록 카드 클릭. */
        private fun openChallenge(challengeId: String) {
            val position = currentState.items.indexOfFirst { it.challengeId == challengeId }
            bizLogger.record(
                ChallengeEvents.challengeCardClick(
                    challengeId = challengeId,
                    position = position.coerceAtLeast(0),
                    source = ChallengeCardSource.LIST,
                    sort = currentState.sort,
                ),
            )
            navigationHelper.navigateByRoute(ChallengeDetailPage(challengeId).toRoute())
        }

        /** 첫 페이지 결과가 나온 뒤에야 보낼 수 있는 이벤트. */
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
            refreshing: Boolean = false,
        ) {
            viewModelScope.launch {
                dispatch(
                    if (refreshing) ExploreListReducerEvent.Refreshing else ExploreListReducerEvent.Loading(filter = filter, sort = sort),
                )
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
                        // 빈 결과는 filter_apply 와 중복으로 보낸다
                        if (result.items.isEmpty()) {
                            bizLogger.record(ChallengeEvents.exploreEmptyResult(filter, sort))
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
                    bizLogger.record(ChallengeEvents.exploreFilterApply(log.filter, resultCount))

                is LogAfterLoad.SortChanged ->
                    bizLogger.record(ChallengeEvents.exploreSortChange(log.from, log.to, resultCount))
            }
        }

        /** 서버가 조건을 거절하면 사용자에게 되묻지 않고 스스로 고쳐 다시 조회한다. */
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

        // 예외 원문(영문 UnknownHostException 등)을 그대로 쓰면 사용자 화면에 노출된다.
        private fun fail(error: Throwable) {
            val message = if (error is IOException) "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요." else "챌린지를 불러오지 못했어요"
            dispatch(ExploreListReducerEvent.Failed(message))
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
                    bizLogger.record(ChallengeEvents.exploreListLoadMore(pageIndex, currentState.sort))
                }.onFailure { error ->
                    // 커서가 상해 있으면 조용히 첫 페이지부터 다시 받는다
                    if (error is CursorInvalidException) {
                        fetchFirstPage(currentState.filter, currentState.sort)
                    } else {
                        dispatch(ExploreListReducerEvent.LoadMoreFailed)
                    }
                }
            }
        }
    }
