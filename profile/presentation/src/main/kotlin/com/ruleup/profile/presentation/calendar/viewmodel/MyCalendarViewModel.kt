package com.ruleup.profile.presentation.calendar.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.time.ServiceDate
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** 활동 캘린더 ViewModel. */
@HiltViewModel
class MyCalendarViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyCalendarIntent, MyCalendarState, MyCalendarReducerEvent, NoEffect>(
            MyCalendarState.initial,
        ) {
        // month(YYYY-MM) → days.
        private val monthCache = mutableMapOf<String, Map<String, CalendarDay>>()

        override fun onIntent(intent: MyCalendarIntent) {
            when (intent) {
                is MyCalendarIntent.Load -> loadInitial(intent.date)
                is MyCalendarIntent.ChangeMonth -> changeMonth(intent.delta)
                MyCalendarIntent.Retry -> changeMonth(0)
                is MyCalendarIntent.SelectDate -> selectDate(intent.date)
                is MyCalendarIntent.OpenAppeal ->
                    navigationHelper.navigateTo(ChallengeDetailPage(intent.challengeId))

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
                        state.copy(
                            isLoading = false,
                            days = event.days,
                            errorMessage = null,
                            selectedDay =
                                if (state.selectedDate?.take(7) ==
                                    event.month
                                ) {
                                    event.days[state.selectedDate]
                                } else {
                                    state.selectedDay
                                },
                        )
                    } else {
                        // 연타로 월이 이미 바뀌었으면 늦게 도착한 응답은 버린다.
                        state
                    }

                is MyCalendarReducerEvent.MonthFailed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is MyCalendarReducerEvent.DateSelected ->
                    state.copy(selectedDate = event.date, selectedDay = state.days[event.date], dayDetail = null)

                is MyCalendarReducerEvent.DetailLoading -> state.copy(isLoadingDetail = event.loading)

                is MyCalendarReducerEvent.DetailLoaded -> state.copy(dayDetail = event.detail)
            }

        private fun loadInitial(date: String?) {
            if (currentState.month.isNotBlank()) return
            // 딥링크가 준 날짜는 서버 문자열이라 형식을 믿지 않는다
            val target = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: ServiceDate.today()
            loadMonth(YearMonth.from(target).toString())
            selectDate(target.toString())
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
                runCatching { myPageRepository.getCalendar(month) }
                    .onSuccess { calendar ->
                        val days = calendar.days.associateBy { it.date }
                        // 당월은 인증 확정마다 갱신되므로 캐시하지 않는다.
                        if (month < YearMonth.from(ServiceDate.today()).toString()) monthCache[month] = days
                        dispatch(MyCalendarReducerEvent.MonthLoaded(month, days))
                    }.onFailure {
                        dispatch(
                            MyCalendarReducerEvent.MonthFailed(
                                if (it is IOException) {
                                    "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
                                } else {
                                    it.message
                                        ?: "캘린더를 불러오지 못했어요"
                                },
                            ),
                        )
                    }
            }
        }

        private fun selectDate(date: String) {
            dispatch(MyCalendarReducerEvent.DateSelected(date))
            // 비대상일(응답 days 에 없음)은 조회 없이 빈 상태를 보여준다.
            if (!currentState.isLoading && currentState.days[date] == null && currentState.month == date.take(7)) return
            viewModelScope.launch {
                dispatch(MyCalendarReducerEvent.DetailLoading(true))
                runCatching { myPageRepository.getCalendarDay(date) }
                    .onSuccess { detail ->
                        // 상세가 도착하기 전에 다른 날짜를 골랐으면 버린다.
                        if (currentState.selectedDate == date) {
                            dispatch(MyCalendarReducerEvent.DetailLoaded(detail))
                        }
                    }.onFailure {
                        if (currentState.selectedDate == date) dispatch(MyCalendarReducerEvent.DetailLoaded(null))
                    }
                if (currentState.selectedDate == date) dispatch(MyCalendarReducerEvent.DetailLoading(false))
            }
        }
    }
