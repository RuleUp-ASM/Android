package com.ruleup.profile.presentation.tier.viewmodel

import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangePage
import com.ruleup.profile.domain.entity.ScoreChangeReason
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 티어 히스토리. 원천이 둘이다 — 그래프(월말 스냅샷)와 점수 변동 이력.
 *
 * 보관이 1년이라 **기본 조회 범위가 곧 전량**이다 — 범위를 좁게 물으면 남아 있는 기록을 화면이
 * 스스로 잘라 버린다. 이력은 서버 고정 50건이라 커서로 이어 붙는다.
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
            val repo = FakeMyPageRepository(tierHistory = { history() }, scoreChanges = { page() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals(listOf(MyPageRepository.MAX_HISTORY_MONTHS), repo.historyMonths)
        }

    @Test
    fun `불러오면 월말 기록을 화면에 올리고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(tierHistory = { history() }, scoreChanges = { page() }))

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
            val repo = FakeMyPageRepository(tierHistory = { history() }, scoreChanges = { page() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierHistoryIntent.Load)
            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals(1, repo.calls.count { it == "getTierHistory" })
        }

    @Test
    fun `둘 다 실패해야 오류로 접는다`() =
        runTest {
            val viewModel =
                viewModel(
                    FakeMyPageRepository(
                        tierHistory = { throw IllegalStateException("그래프 실패") },
                        scoreChanges = { throw IllegalStateException("서버 오류") },
                    ),
                )

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `그래프만 실패해도 이력은 화면에 올린다`() =
        runTest {
            // 두 API 가 나뉘어 있으므로 한쪽이 죽었다고 다른 쪽을 감추면 볼 수 있는 것까지 잃는다.
            val viewModel =
                viewModel(
                    FakeMyPageRepository(
                        tierHistory = { throw IllegalStateException("그래프 실패") },
                        scoreChanges = { page() },
                    ),
                )

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertEquals(1, viewModel.uiState.value.changes.size)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `이력만 실패해도 그래프는 화면에 올린다`() =
        runTest {
            val viewModel =
                viewModel(
                    FakeMyPageRepository(
                        tierHistory = { history() },
                        scoreChanges = { throw IllegalStateException("이력 실패") },
                    ),
                )

            viewModel.onIntent(MyTierHistoryIntent.Load)

            assertNotNull(viewModel.uiState.value.history)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `더 읽기는 이전 응답의 커서로만 묻는다`() =
        runTest {
            // 커서를 클라가 만들어 보내면 서버가 400 으로 막는다 — 불투명 문자열이라 해석하지 않는다.
            val repo =
                FakeMyPageRepository(
                    tierHistory = { history() },
                    scoreChanges = { cursor -> if (cursor == null) page(next = "c2") else page() },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierHistoryIntent.Load)
            viewModel.onIntent(MyTierHistoryIntent.LoadMore)

            assertEquals(listOf(null, "c2"), repo.changeCursors)
            assertEquals(2, viewModel.uiState.value.changes.size)
        }

    @Test
    fun `다음 커서가 없으면 더 묻지 않는다`() =
        runTest {
            val repo = FakeMyPageRepository(tierHistory = { history() }, scoreChanges = { page() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyTierHistoryIntent.Load)
            viewModel.onIntent(MyTierHistoryIntent.LoadMore)

            assertEquals(1, repo.calls.count { it == "getScoreChanges" })
        }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyTierHistoryViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun page(next: String? = null) =
        ScoreChangePage(
            items =
                listOf(
                    ScoreChange(
                        date = "2026-09-07",
                        reason = ScoreChangeReason.CYCLE_SUCCESS,
                        challengeId = "c1",
                        challengeTitle = "아침 6:30 기상",
                        delta = 8,
                    ),
                ),
            nextCursor = next,
            retentionDays = 365,
        )

    private fun history() =
        TierHistory(
            best = null,
            monthly = listOf(TierSnapshot(month = "2026-06", endTier = Tier.GOLD, endScore = 302)),
            retentionNote = "1년 보관",
        )
}
