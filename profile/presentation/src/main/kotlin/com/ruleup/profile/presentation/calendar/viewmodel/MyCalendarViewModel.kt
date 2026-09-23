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

/**
 * 활동 캘린더 ViewModel. day status 는 서버 판정 값 그대로 렌더링한다.
 * 과거 월은 확정 후 변하지 않아 세션 동안 캐시하고(스펙: 과거 월 캐시), 당월만 재진입마다 다시 조회한다.
 */
@HiltViewModel
class MyCalendarViewModel
    @Inject
    constructor(
        private val myPageRepository: MyPageRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyCalendarIntent, MyCalendarState, MyCalendarReducerEvent, NoEffect>(
            MyCalendarState.initial,
        ) {
        // month(YYYY-MM) → days. 과거 월 전용 캐시.
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

        private fun loadInitial(date: String?) {
            if (currentState.month.isNotBlank()) return
            // 딥링크가 준 날짜는 서버 문자열이라 형식을 믿지 않는다 — 파싱에 실패하면 오늘로 연다.
            // 어느 날짜가 「오늘」인지는 판정 기준(KST)을 따른다 — 기기 기준으로 고르면 자정 근처에
            // 서버가 아직 판정하지 않은 날을 오늘이라고 펴게 된다.
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
                        // 당월은 인증 확정마다 갱신되므로 캐시하지 않는다 (스펙: 과거 월 캐시).
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
            if (currentState.days[date] == null && currentState.month == date.take(7)) return
            viewModelScope.launch {
                dispatch(MyCalendarReducerEvent.DetailLoading(true))
                runCatching { myPageRepository.getCalendarDay(date) }
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
