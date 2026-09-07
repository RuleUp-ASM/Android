package com.ruleup.profile.presentation.tier.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.entity.ScoreChangePage
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 티어 히스토리 ViewModel. 그래프(월말 스냅샷)와 점수 변동 이력을 **각각** 읽는다.
 *
 * 둘을 한 요청으로 묶지 않는 이유는 명세가 API 를 나눠 뒀기 때문이고, 나눈 이유는 페이징 단위가
 * 다르기 때문이다 — 그래프는 1년치가 한 번에 오고 이력은 50건씩 이어 붙는다.
 *
 * 그래프 조회가 실패해도 이력은 그린다. 하락 사유는 그래프에 표기하지 않는 것이 정책이고
 * (마이페이지 §2-5), 그 제한은 **그래프 한정**으로 축소됐다(2026-09-07) — 이력에는 사유가 있다.
 */
@HiltViewModel
class MyTierHistoryViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyTierHistoryIntent, MyTierHistoryState, MyTierHistoryReducerEvent, NoEffect>(
            MyTierHistoryState.initial,
        ) {
        override fun onIntent(intent: MyTierHistoryIntent) {
            when (intent) {
                MyTierHistoryIntent.Load -> load()
                MyTierHistoryIntent.LoadMore -> loadMore()
                MyTierHistoryIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyTierHistoryState,
            event: MyTierHistoryReducerEvent,
        ): MyTierHistoryState =
            when (event) {
                MyTierHistoryReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyTierHistoryReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        history = event.history,
                        changes = event.changes,
                        nextCursor = event.nextCursor,
                        retentionDays = event.retentionDays,
                        errorMessage = null,
                    )

                MyTierHistoryReducerEvent.LoadingMore -> state.copy(isLoadingMore = true)

                is MyTierHistoryReducerEvent.MoreLoaded ->
                    state.copy(
                        isLoadingMore = false,
                        changes = state.changes + event.changes,
                        nextCursor = event.nextCursor,
                    )

                // 더 읽기 실패는 이미 그려진 목록을 지우지 않는다 — 끝에 닿으면 다시 시도된다.
                MyTierHistoryReducerEvent.MoreFailed -> state.copy(isLoadingMore = false)

                is MyTierHistoryReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.history != null || currentState.changes.isNotEmpty()) return
            dispatch(MyTierHistoryReducerEvent.Loading)
            viewModelScope.launch {
                val history = runCatching { myPageRepository.getTierHistory() }.getOrNull()
                val changes = runCatching { myPageRepository.getScoreChanges() }

                changes
                    .onSuccess { dispatch(loaded(history, it)) }
                    .onFailure { failure ->
                        // 이력이 실패해도 그래프만 왔으면 그걸로 화면이 성립한다.
                        if (history != null) {
                            dispatch(loaded(history, ScoreChangePage(emptyList(), null, null)))
                        } else {
                            dispatch(MyTierHistoryReducerEvent.Failed(failure.message ?: "히스토리를 불러오지 못했어요"))
                        }
                    }
            }
        }

        private fun loaded(
            history: TierHistory?,
            page: ScoreChangePage,
        ) = MyTierHistoryReducerEvent.Loaded(
            history = history,
            changes = page.items,
            nextCursor = page.nextCursor,
            retentionDays = page.retentionDays,
        )

        private fun loadMore() {
            val cursor = currentState.nextCursor ?: return
            if (currentState.isLoadingMore) return
            dispatch(MyTierHistoryReducerEvent.LoadingMore)
            viewModelScope.launch {
                runCatching { myPageRepository.getScoreChanges(cursor) }
                    .onSuccess {
                        dispatch(MyTierHistoryReducerEvent.MoreLoaded(changes = it.items, nextCursor = it.nextCursor))
                    }.onFailure { dispatch(MyTierHistoryReducerEvent.MoreFailed) }
            }
        }
    }
