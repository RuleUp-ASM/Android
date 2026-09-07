package com.ruleup.profile.presentation.stats.viewmodel

import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyStatsIntent : MviIntent {
    /** 화면 진입 — 지표 5종 일괄 조회. 기간 탭은 없다(명세가 지표를 고정했다). */
    data object Load : MyStatsIntent

    data object Back : MyStatsIntent
}

data class MyStatsState(
    val isLoading: Boolean,
    val report: StatsReport?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyStatsState(
                isLoading = true,
                report = null,
                errorMessage = null,
            )
    }
}

sealed interface MyStatsReducerEvent : ReducerEvent {
    data object Loading : MyStatsReducerEvent

    data class Loaded(
        val report: StatsReport,
    ) : MyStatsReducerEvent

    data class Failed(
        val message: String,
    ) : MyStatsReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyStatsEffect = NoEffect
