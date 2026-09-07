package com.ruleup.profile.presentation.calendar.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.presentation.fake.FakeMyPageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 활동 캘린더. 월을 오가는 화면이라 **어떤 월을 다시 묻고 어떤 월은 묻지 않는지**가 계약이다 —
 * 지난 달 기록은 더 바뀌지 않지만 이번 달은 인증이 확정될 때마다 바뀌므로, 이번 달까지 캐시하면
 * 방금 성공한 인증이 캘린더에 안 나타난다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyCalendarViewModelTest {
    private val thisMonth = YearMonth.from(LocalDate.now()).toString()
    private val lastMonth = YearMonth.from(LocalDate.now()).minusMonths(1).toString()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `처음 열면 이번 달과 오늘을 고른 상태로 시작한다`() =
        runTest {
            val viewModel = viewModel(repo())

            viewModel.onIntent(MyCalendarIntent.Load)

            assertEquals(thisMonth, viewModel.uiState.value.month)
            assertEquals(LocalDate.now().toString(), viewModel.uiState.value.selectedDate)
        }

    @Test
    fun `이미 열려 있으면 다시 열어도 초기화하지 않는다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyCalendarIntent.Load)
            viewModel.onIntent(MyCalendarIntent.ChangeMonth(-1))
            val before = repo.calendarMonths.size

            viewModel.onIntent(MyCalendarIntent.Load)

            assertEquals(lastMonth, viewModel.uiState.value.month)
            assertEquals(before, repo.calendarMonths.size)
        }

    @Test
    fun `달을 넘기면 그 달을 조회한다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyCalendarIntent.Load)

            viewModel.onIntent(MyCalendarIntent.ChangeMonth(-1))

            assertEquals(lastMonth, viewModel.uiState.value.month)
            assertTrue(repo.calendarMonths.contains(lastMonth))
        }

    @Test
    fun `지난 달로 되돌아오면 다시 묻지 않는다`() =
        runTest {
            // 지난 달 기록은 더 바뀌지 않는다 — 오갈 때마다 왕복하면 낭비다.
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyCalendarIntent.Load)
            viewModel.onIntent(MyCalendarIntent.ChangeMonth(-1))
            viewModel.onIntent(MyCalendarIntent.ChangeMonth(1))

            viewModel.onIntent(MyCalendarIntent.ChangeMonth(-1))

            assertEquals(1, repo.calendarMonths.count { it == lastMonth })
        }

    @Test
    fun `이번 달로 돌아오면 다시 묻는다`() =
        runTest {
            // 인증이 확정될 때마다 바뀐다. 캐시하면 방금 성공한 인증이 캘린더에 안 나타난다.
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyCalendarIntent.Load)
            viewModel.onIntent(MyCalendarIntent.ChangeMonth(-1))

            viewModel.onIntent(MyCalendarIntent.ChangeMonth(1))

            assertEquals(2, repo.calendarMonths.count { it == thisMonth })
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(calendar = { throw IllegalStateException("캘린더 오류") }))

            viewModel.onIntent(MyCalendarIntent.Load)

            assertEquals("캘린더 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `인증 대상이 아닌 날은 상세를 묻지 않는다`() =
        runTest {
            // 응답에 없는 날짜는 애초에 할 일이 없던 날이다. 물어봐야 빈 답이 온다.
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyCalendarIntent.Load)
            val before = repo.calls.count { it == "getCalendarDay" }

            viewModel.onIntent(MyCalendarIntent.SelectDate("$thisMonth-28"))

            assertEquals(before, repo.calls.count { it == "getCalendarDay" })
            assertNull(viewModel.uiState.value.dayDetail)
        }

    @Test
    fun `대상일을 고르면 그 날 상세를 가져온다`() =
        runTest {
            val target = "$thisMonth-01"
            val repo = repo(days = listOf(day(target)))
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyCalendarIntent.Load)

            viewModel.onIntent(MyCalendarIntent.SelectDate(target))

            assertEquals(
                target,
                viewModel.uiState.value.dayDetail
                    ?.date,
            )
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(repo(), nav).onIntent(MyCalendarIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun repo(days: List<CalendarDay> = emptyList()) =
        FakeMyPageRepository(
            calendar = { month -> ActivityCalendar(month = month, days = days) },
            calendarDay = { date -> CalendarDayDetail(date = date, items = emptyList()) },
        )

    private fun day(date: String) = CalendarDay(date = date, status = null, successCount = 1, targetCount = 1)

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyCalendarViewModel(myPageRepository = repo, navigationHelper = nav)
}
