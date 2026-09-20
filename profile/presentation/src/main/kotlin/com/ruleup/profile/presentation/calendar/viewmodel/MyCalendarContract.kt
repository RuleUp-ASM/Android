package com.ruleup.profile.presentation.calendar.viewmodel

import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyCalendarIntent : MviIntent {
    /** 화면 진입 — 당월 조회 + 오늘 선택. */
    data object Load : MyCalendarIntent

    /** 월 이동 (delta = ±1). */
    data class ChangeMonth(
        val delta: Int,
    ) : MyCalendarIntent

    /** 일자 탭 → 판정 대상일이면 상세 조회. */
    data class SelectDate(
        val date: String,
    ) : MyCalendarIntent

    /** 조회에 실패한 달을 다시 불러온다. */
    data object Retry : MyCalendarIntent

    /**
     * D+1 유예 중인 실패 건의 이의로 간다. 시트가 아니라 **방 상세로 보낸다** — 이의 시트는
     * challenge:presentation 에 있고 presentation 끼리는 의존하지 않는다. 복제하면 정책 문구가
     * 두 벌로 갈라져 한쪽만 고쳐진다.
     */
    data class OpenAppeal(
        val challengeId: String,
    ) : MyCalendarIntent

    data object Back : MyCalendarIntent
}

data class MyCalendarState(
    // YYYY-MM (표시 중인 월)
    val month: String,
    val isLoading: Boolean,
    // date(YYYY-MM-DD) → 일자 상태. 응답에 없는 날짜는 비대상일.
    val days: Map<String, CalendarDay>,
    // 선택한 일자 (YYYY-MM-DD)
    val selectedDate: String?,
    // 선택 일자 상세 (비대상일이면 null 유지)
    val dayDetail: CalendarDayDetail?,
    val isLoadingDetail: Boolean,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            MyCalendarState(
                month = "",
                isLoading = true,
                days = emptyMap(),
                selectedDate = null,
                dayDetail = null,
                isLoadingDetail = false,
                errorMessage = null,
            )
    }
}

sealed interface MyCalendarReducerEvent : ReducerEvent {
    data class MonthLoading(
        val month: String,
    ) : MyCalendarReducerEvent

    data class MonthLoaded(
        val month: String,
        val days: Map<String, CalendarDay>,
    ) : MyCalendarReducerEvent

    data class MonthFailed(
        val message: String,
    ) : MyCalendarReducerEvent

    data class DateSelected(
        val date: String,
    ) : MyCalendarReducerEvent

    data class DetailLoading(
        val loading: Boolean,
    ) : MyCalendarReducerEvent

    data class DetailLoaded(
        val detail: CalendarDayDetail?,
    ) : MyCalendarReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias MyCalendarEffect = NoEffect
