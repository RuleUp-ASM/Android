package com.ruleup.profile.presentation.stats.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.StatsStreak
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
 * 통계 리포트. 지표 5종이 고정되면서 **기간 탭이 사라져** 조회가 진입 시 한 번으로 줄었다 —
 * 그 절약이 실제로 성립하는지가 이 화면의 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyStatsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 지표를 화면에 올리고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(stats = { report() }))

            viewModel.onIntent(MyStatsIntent.Load)

            assertEquals(
                142,
                viewModel.uiState.value.report
                    ?.totalSuccessCount,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `이미 받아둔 지표가 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(stats = { report() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyStatsIntent.Load)
            viewModel.onIntent(MyStatsIntent.Load)

            assertEquals(1, repo.calls.count { it == "getStats" })
        }

    @Test
    fun `조회에 실패하면 사유를 남기고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(stats = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(MyStatsIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyStatsViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun report() =
        StatsReport(
            successRate = 0.87,
            totalSuccessCount = 142,
            streak = StatsStreak(current = 6, best = 21),
            cycles12w = emptyList(),
            completedCount = 24,
            weeklyScoreDelta = 5,
        )
}
