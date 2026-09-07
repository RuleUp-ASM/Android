package com.ruleup.profile.presentation.appeals.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.verification.domain.entity.AppealHistoryItem
import com.ruleup.verification.domain.test.FakeVerificationRepository
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
 * 이의 내역. 이 화면은 **한 번 실패해도 곧바로 한 번 더 시도한다**(테크스펙 4-6) — 일시적 실패로
 * "이의를 낸 적 없다"처럼 보이는 빈 화면을 보여주지 않으려는 것이다. 그 재시도가 실제로 도는지,
 * 그리고 두 번 다 실패했을 때 조용히 비지 않는지가 이 화면의 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyAppealsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 이의 내역을 화면에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeVerificationRepository(myAppeals = { listOf(item("ap1")) }))

            viewModel.onIntent(MyAppealsIntent.Load)

            assertEquals(
                listOf("ap1"),
                viewModel.uiState.value.history
                    .map { it.appealId },
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `한 번 실패하면 곧바로 한 번 더 시도한다`() =
        runTest {
            // 일시적 실패로 "이의를 낸 적 없다"처럼 보이는 빈 화면을 보여주지 않는다.
            var attempt = 0
            val repo =
                FakeVerificationRepository(
                    myAppeals = {
                        attempt++
                        if (attempt == 1) throw IllegalStateException("일시 오류") else listOf(item("ap1"))
                    },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyAppealsIntent.Load)

            assertEquals(2, attempt)
            assertEquals(
                listOf("ap1"),
                viewModel.uiState.value.history
                    .map { it.appealId },
            )
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `두 번 다 실패하면 빈 화면 대신 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeVerificationRepository(myAppeals = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(MyAppealsIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
            assertEquals(emptyList(), viewModel.uiState.value.history)
        }

    @Test
    fun `재시도는 세 번째 요청을 만들지 않는다`() =
        runTest {
            // 자동 재시도는 1회다. 더 늘리면 사용자가 기다리는 시간만 배로 늘어난다.
            val repo = FakeVerificationRepository(myAppeals = { throw IllegalStateException("서버 오류") })

            viewModel(repo).onIntent(MyAppealsIntent.Load)

            assertEquals(2, repo.calls.count { it == "getMyAppeals" })
        }

    @Test
    fun `이미 받아둔 내역이 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeVerificationRepository(myAppeals = { listOf(item("ap1")) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyAppealsIntent.Load)
            viewModel.onIntent(MyAppealsIntent.Load)

            assertEquals(1, repo.calls.count { it == "getMyAppeals" })
        }

    @Test
    fun `다시 시도하기는 받아둔 내역이 있어도 새로 묻는다`() =
        runTest {
            // 사용자가 명시적으로 누른 것이라 캐시를 이유로 무시하면 안 된다.
            val repo = FakeVerificationRepository(myAppeals = { listOf(item("ap1")) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyAppealsIntent.Load)

            viewModel.onIntent(MyAppealsIntent.Retry)

            assertEquals(2, repo.calls.count { it == "getMyAppeals" })
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(MyAppealsIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun viewModel(
        repo: FakeVerificationRepository = FakeVerificationRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyAppealsViewModel(verificationRepository = repo, navigationHelper = nav)

    private fun item(id: String) =
        AppealHistoryItem(
            appealId = id,
            date = "2026-08-30",
            challengeId = "ch1",
            routineTitle = "아침 6시 기상",
            reason = "알람이 울리지 않았어요",
            track = null,
        )
}
