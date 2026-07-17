package com.ruleup.profile.presentation.calendar.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.usecase.GetActivityCalendarUseCase
import com.ruleup.profile.domain.usecase.GetCalendarDayDetailUseCase
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 활동 캘린더 ViewModel. day status 는 서버 판정 값 그대로 렌더링한다.
 * 과거 월은 확정 후 변하지 않으므로(스펙: 과거 월 캐시) 세션 동안 메모리에 캐시하고,
 * 당월만 재진입 시 다시 조회한다.
 */
@HiltViewModel
class MyCalendarViewModel
    @Inject
    constructor(
        private val getActivityCalendarUseCase: GetActivityCalendarUseCase,
        private val getCalendarDayDetailUseCase: GetCalendarDayDetailUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyCalendarIntent, MyCalendarState, MyCalendarReducerEvent, NoEffect>(
            MyCalendarState.initial,
        ) {
        // month(YYYY-MM) → days. 과거 월 전용 캐시.
        private val monthCache = mutableMapOf<String, Map<String, CalendarDay>>()

        override fun onIntent(intent: MyCalendarIntent) {
            when (intent) {
                MyCalendarIntent.Load -> loadInitial()
                is MyCalendarIntent.ChangeMonth -> changeMonth(intent.delta)
                is MyCalendarIntent.SelectDate -> selectDate(intent.date)
                MyCalendarIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: MyCalendarState,
            event: MyCalendarReducerEvent,
        ): MyCalendarState =
            when (event) {
                is MyCalendarReducerEvent.MonthLoading ->
                    state.copy(isLoading = true, month = event.month, errorMessage = null)

                is MyCalendarReducerEvent.MonthLoaded ->
                    if (state.month == event.month) {
                        state.copy(isLoading = false, days = event.days, errorMessage = null)
                    } else {
                        // 연타로 월이 이미 바뀌었으면 늦게 도착한 응답은 버린다.
                        state
                    }

                is MyCalendarReducerEvent.MonthFailed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is MyCalendarReducerEvent.DateSelected ->
                    state.copy(selectedDate = event.date, dayDetail = null)

                is MyCalendarReducerEvent.DetailLoading -> state.copy(isLoadingDetail = event.loading)

                is MyCalendarReducerEvent.DetailLoaded -> state.copy(dayDetail = event.detail)
            }

        private fun loadInitial() {
            if (currentState.month.isNotBlank()) return
            val today = LocalDate.now()
            loadMonth(YearMonth.from(today).toString())
            selectDate(today.toString())
        }

        private fun changeMonth(delta: Int) {
            val current = runCatching { YearMonth.parse(currentState.month) }.getOrNull() ?: return
            loadMonth(current.plusMonths(delta.toLong()).toString())
        }

        private fun loadMonth(month: String) {
            val cached = monthCache[month]
            if (cached != null) {
                dispatch(MyCalendarReducerEvent.MonthLoading(month))
                dispatch(MyCalendarReducerEvent.MonthLoaded(month, cached))
                return
            }
            dispatch(MyCalendarReducerEvent.MonthLoading(month))
            viewModelScope.launch {
                runCatching { getActivityCalendarUseCase(month) }
                    .onSuccess { calendar ->
                        val days = calendar.days.associateBy { it.date }
                        // 당월은 인증 확정마다 갱신되므로 캐시하지 않는다 (스펙: 과거 월 캐시).
                        if (month < YearMonth.from(LocalDate.now()).toString()) monthCache[month] = days
                        dispatch(MyCalendarReducerEvent.MonthLoaded(month, days))
                    }.onFailure {
                        dispatch(MyCalendarReducerEvent.MonthFailed(it.message ?: "캘린더를 불러오지 못했어요"))
                    }
            }
        }

        private fun selectDate(date: String) {
            dispatch(MyCalendarReducerEvent.DateSelected(date))
            // 비대상일(응답 days 에 없음)은 조회 없이 빈 상태를 보여준다.
            if (currentState.days[date] == null && currentState.month == date.take(7)) return
            viewModelScope.launch {
                dispatch(MyCalendarReducerEvent.DetailLoading(true))
                runCatching { getCalendarDayDetailUseCase(date) }
                    .onSuccess { detail ->
                        // 상세가 도착하기 전에 다른 날짜를 골랐으면 버린다.
                        if (currentState.selectedDate == date) {
                            dispatch(MyCalendarReducerEvent.DetailLoaded(detail))
                        }
                    }.onFailure { dispatch(MyCalendarReducerEvent.DetailLoaded(null)) }
                dispatch(MyCalendarReducerEvent.DetailLoading(false))
            }
        }
    }
