package com.ruleup.challenge.presentation.ranking.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.RankingEntry
import com.ruleup.challenge.presentation.fake.FakeRoomRepository
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserRelationship
import com.ruleup.domain.test.RecordingNavigationHelper
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

/** 그룹 랭킹. */
@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 서버가 준 순서 그대로 보여 준다`() =
        runTest {
            val viewModel = viewModel(FakeRoomRepository(ranking = { ranking("u3", "u1", "u2") }))

            viewModel.onIntent(RankingIntent.Load("ch1"))

            assertEquals(
                listOf("u3", "u1", "u2"),
                viewModel.uiState.value.ranking
                    ?.items
                    ?.map { it.user.id },
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `등재되지 않은 내 순위를 0등으로 접지 않는다`() =
        runTest {
            // 10회 미만은 미등재다.
            val viewModel =
                viewModel(
                    FakeRoomRepository(
                        ranking = { ChallengeRanking(me = unranked(), items = emptyList()) },
                    ),
                )

            viewModel.onIntent(RankingIntent.Load("ch1"))

            assertNull(
                viewModel.uiState.value.ranking
                    ?.me
                    ?.rank,
            )
            assertNull(
                viewModel.uiState.value.ranking
                    ?.me
                    ?.successRate,
            )
        }

    @Test
    fun `다른 방을 열면 그 방 랭킹을 다시 묻는다`() =
        runTest {
            val repo = FakeRoomRepository(ranking = { ranking("u1") })
            val viewModel = viewModel(repo)

            viewModel.onIntent(RankingIntent.Load("ch1"))
            viewModel.onIntent(RankingIntent.Load("ch2"))

            assertEquals(2, repo.calls.count { it == "getRanking" })
            assertEquals("ch2", viewModel.uiState.value.challengeId)
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeRoomRepository(ranking = { throw IllegalStateException("랭킹 오류") }))

            viewModel.onIntent(RankingIntent.Load("ch1"))

            assertEquals("랭킹을 불러오지 못했어요", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(RankingIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun viewModel(
        repo: FakeRoomRepository = FakeRoomRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = RankingViewModel(roomRepository = repo, navigationHelper = nav)

    private fun ranking(vararg userIds: String) =
        ChallengeRanking(
            me = MyRank(rank = 1, ranked = true, successRate = 0.9, participations = 12, gapToFirst = 0.0),
            items =
                userIds.mapIndexed { index, id ->
                    RankingEntry(
                        rank = index + 1,
                        user =
                            User(
                                id = id,
                                nickname = id,
                                profileImageUrl = null,
                                relationship = UserRelationship(blocked = false),
                            ),
                        successRate = 0.9,
                        successCount = 9,
                        participations = 10,
                    )
                },
        )

    private fun unranked() = MyRank(rank = null, ranked = false, successRate = null, participations = 3, gapToFirst = null)
}
