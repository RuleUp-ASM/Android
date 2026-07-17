package com.ruleup.profile.presentation.stats.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.usecase.GetStatsReportUseCase
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 통계 리포트 ViewModel. 집계는 서버 온디맨드 — 기간 탭 전환마다 조회한다. */
@HiltViewModel
class MyStatsViewModel
    @Inject
    constructor(
        private val getStatsReportUseCase: GetStatsReportUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyStatsIntent, MyStatsState, MyStatsReducerEvent, NoEffect>(
            MyStatsState.initial,
        ) {
        override fun onIntent(intent: MyStatsIntent) {
            when (intent) {
                MyStatsIntent.Load -> if (currentState.report == null) load(currentState.period)
                is MyStatsIntent.SelectPeriod -> if (intent.period != currentState.period) load(intent.period)
                MyStatsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyStatsState,
            event: MyStatsReducerEvent,
        ): MyStatsState =
            when (event) {
                is MyStatsReducerEvent.Loading ->
                    state.copy(isLoading = true, period = event.period, errorMessage = null)

                is MyStatsReducerEvent.Loaded ->
                    if (state.period == event.period) {
                        state.copy(isLoading = false, report = event.report, errorMessage = null)
                    } else {
                        // 탭 연타로 기간이 이미 바뀌었으면 늦은 응답은 버린다.
                        state
                    }

                is MyStatsReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load(period: StatsPeriod) {
            dispatch(MyStatsReducerEvent.Loading(period))
            viewModelScope.launch {
                runCatching { getStatsReportUseCase(period) }
                    .onSuccess { dispatch(MyStatsReducerEvent.Loaded(period, it)) }
                    .onFailure { dispatch(MyStatsReducerEvent.Failed(it.message ?: "통계를 불러오지 못했어요")) }
            }
        }
    }
