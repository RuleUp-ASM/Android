package com.ruleup.challenge.presentation.mychallenges.viewmodel

import com.ruleup.challenge.domain.entity.LeftType
import com.ruleup.challenge.domain.entity.MyChallengeFilter
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.presentation.mychallenges.myChallenge
import com.ruleup.challenge.presentation.mychallenges.page
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.verification.domain.entity.ProgressSnapshot
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
import kotlin.test.assertTrue

/**
 * 내 챌린지 목록. 서버가 완료와 이탈을 **다른 filter 로** 내리는데 화면은 한 탭에 합쳐 보여 준다 —
 * 그래서 두 응답을 합치고 커서도 각각 들고 있어야 한다. 한쪽만 따라가면 사용자의 지난 방이 조용히
 * 사라진다.
 *
 * 달성률은 목록이 아니라 인증 진행률에서 오므로, 그 조회가 실패해도 목록이 오류 화면이 되면 안 된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyChallengesViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 세 탭을 모두 물어 진행 중과 끝난 방을 따로 채운다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)

            viewModel.onIntent(MyChallengesIntent.Load)

            assertEquals(
                listOf(MyChallengeFilter.IN_PROGRESS, MyChallengeFilter.COMPLETED, MyChallengeFilter.LEFT),
                repo.myChallengeFilters.sortedBy { it.ordinal },
            )
            assertEquals(1, viewModel.uiState.value.inProgress.size)
            assertEquals(2, viewModel.uiState.value.finished.size)
        }

    @Test
    fun `끝난 방은 완료와 이탈을 섞어 종료일 최신순으로 세운다`() =
        runTest {
            // 두 목록을 이어 붙이기만 하면 이탈 건이 전부 아래로 몰려 시간 순서가 깨진다.
            val viewModel = viewModel(repo())

            viewModel.onIntent(MyChallengesIntent.Load)

            assertEquals(
                listOf("2026-07-13", "2026-05-20"),
                viewModel.uiState.value.finished
                    .map { it.period.end },
            )
        }

    @Test
    fun `진행률 조회에 실패해도 목록을 오류 화면으로 바꾸지 않는다`() =
        runTest {
            val viewModel =
                viewModel(
                    repo(),
                    FakeVerificationRepository(progress = { throw IllegalStateException("진행률 조회 실패") }),
                )

            viewModel.onIntent(MyChallengesIntent.Load)

            assertEquals(1, viewModel.uiState.value.inProgress.size)
            assertNull(viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.progress)
        }

    @Test
    fun `목록 조회부터 실패하면 보여 줄 게 없으니 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeChallengeRepository(myChallenges = { _, _ -> throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(MyChallengesIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `다음 장이 없으면 더 부르지 않는다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyChallengesIntent.Load)
            val before = repo.calls.count { it == "getMyChallenges" }

            viewModel.onIntent(MyChallengesIntent.LoadMore)

            assertEquals(before, repo.calls.count { it == "getMyChallenges" })
        }

    @Test
    fun `다음 장이 남은 탭만 커서를 들고 다시 묻는다`() =
        runTest {
            // 완료만 다음 장이 있는 상황이다. 이탈까지 같이 물으면 이미 받은 항목이 두 번 들어온다.
            val repo =
                FakeChallengeRepository(
                    myChallenges = { filter, cursor ->
                        when (filter) {
                            MyChallengeFilter.IN_PROGRESS -> page()
                            MyChallengeFilter.COMPLETED ->
                                if (cursor == null) {
                                    page(myChallenge(id = "done1", end = "2026-07-13"), nextCursor = "c2")
                                } else {
                                    page(myChallenge(id = "done2", end = "2026-04-01"))
                                }

                            MyChallengeFilter.LEFT -> page(myChallenge(id = "left1", end = "2026-05-20", leftType = LeftType.SELF))
                        }
                    },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(MyChallengesIntent.Load)

            viewModel.onIntent(MyChallengesIntent.LoadMore)

            assertEquals(
                listOf("done1", "left1", "done2"),
                viewModel.uiState.value.finished
                    .map { it.challengeId },
            )
            assertTrue(repo.myChallengeCursors.contains("c2"))
            assertFalse(viewModel.uiState.value.finishedPaging.hasNext)
        }

    private fun repo() =
        FakeChallengeRepository(
            myChallenges = { filter, _ ->
                when (filter) {
                    MyChallengeFilter.IN_PROGRESS -> page(myChallenge(id = "going"))
                    MyChallengeFilter.COMPLETED -> page(myChallenge(id = "done", end = "2026-07-13"))
                    MyChallengeFilter.LEFT -> page(myChallenge(id = "left", end = "2026-05-20", leftType = LeftType.SELF))
                }
            },
        )

    private fun viewModel(
        repo: FakeChallengeRepository,
        verification: FakeVerificationRepository =
            FakeVerificationRepository(progress = { ProgressSnapshot(asOf = "2026-09-01T00:00:00Z", challenges = emptyList()) }),
        notifications: FakeNotificationRepository = FakeNotificationRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = MyChallengesViewModel(
        challengeRepository = repo,
        verificationRepository = verification,
        notificationRepository = notifications,
        navigationHelper = nav,
    )
}
