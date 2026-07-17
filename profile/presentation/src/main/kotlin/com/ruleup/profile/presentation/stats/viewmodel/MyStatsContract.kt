package com.ruleup.profile.presentation.stats.viewmodel

import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyStatsIntent : MviIntent {
    /** 화면 진입 — 기본 기간(월간) 조회. */
    data object Load : MyStatsIntent

    /** 기간 탭 전환 (주간/월간/연간). */
    data class SelectPeriod(
        val period: StatsPeriod,
    ) : MyStatsIntent

    data object Back : MyStatsIntent
}

data class MyStatsState(
    val period: StatsPeriod,
    val isLoading: Boolean,
    val report: StatsReport?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyStatsState(
                // 명세 기본값
                period = StatsPeriod.MONTHLY,
                isLoading = true,
                report = null,
                errorMessage = null,
            )
    }
}

sealed interface MyStatsReducerEvent : ReducerEvent {
    data class Loading(
        val period: StatsPeriod,
    ) : MyStatsReducerEvent

    data class Loaded(
        val period: StatsPeriod,
        val report: StatsReport,
    ) : MyStatsReducerEvent

    data class Failed(
        val message: String,
    ) : MyStatsReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyStatsEffect = NoEffect
