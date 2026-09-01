package com.ruleup.profile.presentation.history.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.ReputationHistory
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
import kotlin.test.assertTrue

/** 평판 히스토리. 서버가 한 번에 다 주므로(상한 50건) 화면은 조회 1회와 실패 표시만 책임진다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReputationHistoryViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 최고 온도와 이정표를 화면에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(reputationHistory = { history(peak = 42.0) }))

            viewModel.onIntent(ReputationHistoryIntent.Load)

            assertEquals(
                42.0,
                viewModel.uiState.value.history
                    ?.peakTemperature,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `이미 받아둔 히스토리가 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(reputationHistory = { history() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(ReputationHistoryIntent.Load)
            viewModel.onIntent(ReputationHistoryIntent.Load)

            assertEquals(1, repo.calls.count { it == "getReputationHistory" })
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(reputationHistory = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(ReputationHistoryIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `실패한 뒤에는 다시 불러올 수 있다`() =
        runTest {
            // 성공한 값이 없으면 잠그지 않는다 — 잠그면 사용자가 이 화면에서 영영 못 벗어난다.
            var fail = true
            val repo =
                FakeMyPageRepository(
                    reputationHistory = { if (fail) throw IllegalStateException("일시 오류") else history() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(ReputationHistoryIntent.Load)

            fail = false
            viewModel.onIntent(ReputationHistoryIntent.Load)

            assertTrue(viewModel.uiState.value.history != null)
            assertEquals(2, repo.calls.count { it == "getReputationHistory" })
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(ReputationHistoryIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = ReputationHistoryViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun history(peak: Double = 40.0) =
        ReputationHistory(peakTemperature = peak, peakAchievedAt = "2026-08-01", milestones = emptyList())
}
