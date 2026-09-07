package com.ruleup.profile.presentation.stats.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 통계 리포트 ViewModel. 지표 5종이 고정이라 조회는 진입 시 한 번이다. */
@HiltViewModel
class MyStatsViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyStatsIntent, MyStatsState, MyStatsReducerEvent, NoEffect>(
            MyStatsState.initial,
        ) {
        override fun onIntent(intent: MyStatsIntent) {
            when (intent) {
                MyStatsIntent.Load -> load()
                MyStatsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyStatsState,
            event: MyStatsReducerEvent,
        ): MyStatsState =
            when (event) {
                MyStatsReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyStatsReducerEvent.Loaded ->
                    state.copy(isLoading = false, report = event.report, errorMessage = null)

                is MyStatsReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load() {
            if (currentState.report != null) return
            dispatch(MyStatsReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { myPageRepository.getStats() }
                    .onSuccess { dispatch(MyStatsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(MyStatsReducerEvent.Failed(it.message ?: "통계를 불러오지 못했어요")) }
            }
        }
    }
