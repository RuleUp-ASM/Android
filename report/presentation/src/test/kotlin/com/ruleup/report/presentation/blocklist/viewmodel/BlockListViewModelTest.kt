package com.ruleup.report.presentation.blocklist.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.ReportException
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.presentation.blocklist.fake.FakeReportRepository
import com.ruleup.report.presentation.blocklist.fake.blockedChallenge
import com.ruleup.report.presentation.blocklist.fake.blockedUser
import com.ruleup.report.presentation.blocklist.fake.emptyBlocks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val nav = RecordingNavigationHelper()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeReportRepository) = BlockListViewModel(repo, nav)

    private fun filled() = BlockList(users = listOf(blockedUser()), challenges = listOf(blockedChallenge()))

    @Test
    fun `목록을 불러오면 두 갈래가 상태에 담긴다`() =
        runTest(dispatcher) {
            val model = vm(FakeReportRepository(pages = listOf(filled())))

            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()

            assertEquals(
                "u-1",
                model.uiState.value.blocks.users
                    .single()
                    .userId,
            )
            assertEquals(
                "c-1",
                model.uiState.value.blocks.challenges
                    .single()
                    .challengeId,
            )
            assertFalse(model.uiState.value.isLoading)
        }

    @Test
    fun `차단한 대상이 없으면 빈 상태로 남는다`() =
        runTest(dispatcher) {
            val model = vm(FakeReportRepository(pages = listOf(emptyBlocks())))

            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()

            assertTrue(model.uiState.value.isEmpty)
            assertNull(model.uiState.value.errorMessage)
        }

    @Test
    fun `해제는 확인을 거치기 전에는 서버로 나가지 않는다`() =
        runTest(dispatcher) {
            // 해제를 되돌리려면 다시 신고해야 하고, 그러면 신고 건이 하나 더 쌓인다.
            val repo = FakeReportRepository(pages = listOf(filled()))
            val model = vm(repo)
            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()

            model.onIntent(BlockListIntent.Unblock)
            testScheduler.advanceUntilIdle()

            assertEquals(emptyList(), repo.unblockedUserIds)
        }

    @Test
    fun `확인한 사용자를 해제하고 목록을 다시 불러온다`() =
        runTest(dispatcher) {
            // 로컬에서 행만 지우면 다른 기기에서 생긴 변화가 반영되지 않는다.
            val repo = FakeReportRepository(pages = listOf(filled(), emptyBlocks()))
            val model = vm(repo)
            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()

            model.onIntent(BlockListIntent.ConfirmUnblock(BlockTarget.User("u-1", "임시 이름 4f2a")))
            model.onIntent(BlockListIntent.Unblock)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("u-1"), repo.unblockedUserIds)
            assertEquals(2, repo.loadCount)
            assertTrue(model.uiState.value.isEmpty)
            assertNull(model.uiState.value.confirming)
        }

    @Test
    fun `확인한 챌린지는 챌린지 해제 경로로 나간다`() =
        runTest(dispatcher) {
            val repo = FakeReportRepository(pages = listOf(filled(), emptyBlocks()))
            val model = vm(repo)
            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()

            model.onIntent(BlockListIntent.ConfirmUnblock(BlockTarget.Challenge("c-1", "확인 중인 챌린지")))
            model.onIntent(BlockListIntent.Unblock)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("c-1"), repo.unblockedChallengeIds)
            assertEquals(emptyList(), repo.unblockedUserIds)
        }

    @Test
    fun `해제를 두 번 눌러도 한 번만 나간다`() =
        runTest(dispatcher) {
            // 두 번째는 404 로 돌아와 오류처럼 보인다.
            val repo = FakeReportRepository(pages = listOf(filled(), emptyBlocks()))
            val model = vm(repo)
            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()
            model.onIntent(BlockListIntent.ConfirmUnblock(BlockTarget.User("u-1", "임시 이름")))

            model.onIntent(BlockListIntent.Unblock)
            model.onIntent(BlockListIntent.Unblock)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("u-1"), repo.unblockedUserIds)
        }

    @Test
    fun `이미 풀린 차단은 오류가 아니라 목록이 옛것이라고 알린다`() =
        runTest(dispatcher) {
            // 다른 기기에서 먼저 푼 경우다 — 사용자가 원한 결과는 이미 이뤄져 있다.
            val repo =
                FakeReportRepository(
                    pages = listOf(filled()),
                    unblockError = ReportException(ReportFailure.BLOCK_ENTRY_NOT_FOUND, "없음"),
                )
            val model = vm(repo)
            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()
            model.onIntent(BlockListIntent.ConfirmUnblock(BlockTarget.User("u-1", "임시 이름")))

            model.onIntent(BlockListIntent.Unblock)
            testScheduler.advanceUntilIdle()

            assertEquals("이미 풀린 차단이에요. 목록을 새로 불러올게요.", model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.unblocking)
        }

    @Test
    fun `조회에 실패하면 오류 문구를 남기고 로딩을 끝낸다`() =
        runTest(dispatcher) {
            val model = vm(FakeReportRepository(loadError = IOException("offline")))

            model.onIntent(BlockListIntent.Load)
            testScheduler.advanceUntilIdle()

            assertFalse(model.uiState.value.isLoading)
            assertTrue(
                model.uiState.value.errorMessage!!
                    .isNotBlank(),
            )
        }

    @Test
    fun `뒤로는 화면을 닫는다`() =
        runTest(dispatcher) {
            val model = vm(FakeReportRepository(pages = listOf(emptyBlocks())))

            model.onIntent(BlockListIntent.Back)

            assertEquals(1, nav.backCount)
        }
}
