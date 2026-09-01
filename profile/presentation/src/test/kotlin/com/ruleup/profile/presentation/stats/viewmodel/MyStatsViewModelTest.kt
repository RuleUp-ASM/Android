package com.ruleup.profile.presentation.stats.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.presentation.fake.FakeMyPageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 통계 리포트. 집계를 서버가 기간마다 새로 하므로 **탭을 바꾸면 반드시 다시 물어야** 하고,
 * 같은 탭을 다시 누르면 묻지 않아야 한다. 다른 조회 화면과 재조회 규칙이 반대라 헷갈리기 쉽다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyStatsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `처음 열면 월간을 보여준다`() {
        assertEquals(StatsPeriod.MONTHLY, viewModel().uiState.value.period)
    }

    @Test
    fun `불러오면 화면에 열린 기간으로 조회한다`() =
        runTest {
            val repo = FakeMyPageRepository(stats = { report(it) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyStatsIntent.Load)

            assertEquals(listOf(StatsPeriod.MONTHLY), repo.statsPeriods)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `기간 탭을 바꾸면 그 기간으로 다시 조회한다`() =
        runTest {
            // 집계는 서버가 기간마다 새로 한다 — 바꿔 놓고 안 물으면 이전 기간 숫자가 남는다.
            val repo = FakeMyPageRepository(stats = { report(it) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyStatsIntent.Load)

            viewModel.onIntent(MyStatsIntent.SelectPeriod(StatsPeriod.WEEKLY))

            assertEquals(listOf(StatsPeriod.MONTHLY, StatsPeriod.WEEKLY), repo.statsPeriods)
            assertEquals(StatsPeriod.WEEKLY, viewModel.uiState.value.period)
        }

    @Test
    fun `이미 보고 있는 기간을 다시 누르면 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(stats = { report(it) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyStatsIntent.Load)

            viewModel.onIntent(MyStatsIntent.SelectPeriod(StatsPeriod.MONTHLY))

            assertEquals(1, repo.statsPeriods.size)
        }

    @Test
    fun `이미 받아둔 리포트가 있으면 다시 열어도 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(stats = { report(it) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyStatsIntent.Load)
            viewModel.onIntent(MyStatsIntent.Load)

            assertEquals(1, repo.statsPeriods.size)
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(stats = { throw IllegalStateException("집계 실패") }))

            viewModel.onIntent(MyStatsIntent.Load)

            assertEquals("집계 실패", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(MyStatsIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyStatsViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun report(period: StatsPeriod) =
        StatsReport(
            period = period,
            totalCompleted = 3,
            avgCompletionRate = 72,
            mannerDelta = 1.2,
            avgStreak = 4.0,
            series = emptyList(),
            insight = null,
        )
}
