package com.ruleup.profile.presentation.temperature.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.ReputationDetail
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 매너 온도 상세. 온도는 서버가 하루 한 번 계산하므로 **화면 안에서 값이 바뀔 일이 없다** —
 * 그래서 한 번 받으면 다시 받지 않는다. 그 절약이 실제로 성립하는지가 이 화면의 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyTemperatureViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 온도를 화면에 올리고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(reputation = { detail(36.7) }))

            viewModel.onIntent(MyTemperatureIntent.Load)

            assertEquals(
                36.7,
                viewModel.uiState.value.detail
                    ?.current,
            )
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `이미 받아둔 온도가 있으면 다시 묻지 않는다`() =
        runTest {
            // 하루 한 번 갱신되는 값이라 재진입마다 조회하면 왕복만 늘고 얻는 게 없다.
            val repo = FakeMyPageRepository(reputation = { detail(36.7) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTemperatureIntent.Load)
            viewModel.onIntent(MyTemperatureIntent.Load)

            assertEquals(1, repo.calls.count { it == "getReputation" })
        }

    @Test
    fun `조회에 실패하면 사유를 남기고 빈 화면으로 두지 않는다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(reputation = { throw IllegalStateException("네트워크 끊김") }))

            viewModel.onIntent(MyTemperatureIntent.Load)

            assertEquals("네트워크 끊김", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `사유를 알 수 없는 실패도 사용자에게 할 말은 있어야 한다`() =
        runTest {
            // message 가 없는 예외를 그대로 흘리면 화면에 빈 문자열이 뜬다.
            val viewModel = viewModel(FakeMyPageRepository(reputation = { throw IllegalStateException() }))

            viewModel.onIntent(MyTemperatureIntent.Load)

            assertNotNull(
                viewModel.uiState.value.errorMessage
                    ?.takeIf { it.isNotBlank() },
            )
        }

    @Test
    fun `변동 이력 보기는 히스토리 화면으로 보낸다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(MyTemperatureIntent.OpenHistory)

        assertEquals(1, nav.routes.size)
    }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(MyTemperatureIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyTemperatureViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun detail(current: Double) =
        ReputationDetail(
            current = current,
            bandLabel = "따뜻해요",
            nextTier = null,
            recentChanges = emptyList(),
        )
}
