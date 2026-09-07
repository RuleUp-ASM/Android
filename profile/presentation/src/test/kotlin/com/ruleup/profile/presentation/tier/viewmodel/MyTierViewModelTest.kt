package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.navigation.MyTierHistoryPage
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
import kotlin.test.assertNull

/**
 * 내 티어 상세. 점수는 서버가 판정 직후에만 움직이므로 **화면 안에서 값이 바뀔 일이 없다** —
 * 그래서 한 번 받으면 다시 받지 않는다. 그 절약이 실제로 성립하는지가 이 화면의 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyTierViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 티어를 화면에 올리고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(tier = { tier(score = 370) }))

            viewModel.onIntent(MyTierIntent.Load)

            assertEquals(
                370,
                viewModel.uiState.value.tier
                    ?.score,
            )
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `이미 받아둔 티어가 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(tier = { tier() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierIntent.Load)
            viewModel.onIntent(MyTierIntent.Load)

            assertEquals(1, repo.calls.count { it == "getTier" })
        }

    @Test
    fun `조회에 실패하면 사유를 남기고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(tier = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(MyTierIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `전체 보기는 히스토리 화면으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(FakeMyPageRepository(), nav)

            viewModel.onIntent(MyTierIntent.OpenHistory)

            assertEquals(MyTierHistoryPage.PATH, nav.routes.single().path)
        }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyTierViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun tier(score: Int = 370) =
        MyTier(
            tier = Tier.GOLD,
            score = score,
            displayTier = Tier.GOLD,
            graceBand = false,
            promotion = null,
            demotion = null,
            recentChanges = emptyList(),
        )
}
