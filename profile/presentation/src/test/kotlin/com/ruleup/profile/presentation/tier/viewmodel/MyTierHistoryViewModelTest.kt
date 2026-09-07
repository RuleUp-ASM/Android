package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.entity.TierSnapshot
import com.ruleup.profile.domain.repository.MyPageRepository
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
 * 티어 히스토리. 보관이 1년이라 **기본 조회 범위가 곧 전량**이다 — 여기서 범위를 좁게 물으면
 * 남아 있는 기록을 화면이 스스로 잘라 버린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyTierHistoryViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 보관 기간 전량을 묻는다`() =
        runTest {
            val repo = FakeMyPageRepository(tierHistory = { history() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals(listOf(MyPageRepository.MAX_HISTORY_MONTHS), repo.historyMonths)
        }

    @Test
    fun `불러오면 월말 기록을 화면에 올리고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(tierHistory = { history() }))

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals(
                1,
                viewModel.uiState.value.history
                    ?.monthly
                    ?.size,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `이미 받아둔 기록이 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(tierHistory = { history() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierHistoryIntent.Load)
            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals(1, repo.calls.count { it == "getTierHistory" })
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(tierHistory = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyTierHistoryViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun history() =
        TierHistory(
            best = null,
            monthly = listOf(TierSnapshot(month = "2026-06", endTier = Tier.GOLD, endScore = 302)),
            retentionNote = "1년 보관",
        )
}
