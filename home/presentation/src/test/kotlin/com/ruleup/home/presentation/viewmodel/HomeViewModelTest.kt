package com.ruleup.home.presentation.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
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
import kotlin.test.assertTrue

/**
 * 홈. 서버 목록·진행률·로컬 스토어 **세 출처를 병합**해 카드를 그리는데 각 조회는 따로 실패할 수
 * 있다. 하나가 죽어도 나머지로 홈이 그려져야 한다 — 빈 홈은 "챌린지가 사라졌다"로 읽힌다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 내 챌린지를 카드로 올린다`() =
        runTest {
            val viewModel = viewModel(challenges = listOf(myChallenge("ch1")))

            viewModel.onIntent(HomeIntent.Load)

            assertEquals(
                listOf("ch1"),
                viewModel.uiState.value.challenges
                    .map { it.challengeId },
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `목록 조회가 죽어도 홈을 비우지 않는다`() =
        runTest {
            // 로컬에 남은 "내 챌린지"만으로도 그려야 한다.
            val viewModel =
                viewModel(
                    challenges = null,
                    locals = listOf(local("ch1")),
                )

            viewModel.onIntent(HomeIntent.Load)

            assertEquals(
                listOf("ch1"),
                viewModel.uiState.value.challenges
                    .map { it.challengeId },
            )
        }

    @Test
    fun `진행률 조회가 죽어도 목록은 그대로 그린다`() =
        runTest {
            val viewModel = viewModel(challenges = listOf(myChallenge("ch1")), progress = null)

            viewModel.onIntent(HomeIntent.Load)

            assertEquals(
                listOf("ch1"),
                viewModel.uiState.value.challenges
                    .map { it.challengeId },
            )
        }

    @Test
    fun `이미 불러오는 중이면 다시 요청하지 않는다`() =
        runTest {
            // 홈 재진입마다 LaunchedEffect 가 다시 발화한다 — 막지 않으면 중복 요청이 쌓인다.
            val repo = FakeChallengeRepository(myChallenges = { listOf(myChallenge("ch1")) })
            val viewModel = viewModel(repo = repo)

            viewModel.onIntent(HomeIntent.Load)
            viewModel.onIntent(HomeIntent.Load)

            assertEquals(2, repo.calls.count { it == "getMyChallenges" })
        }

    @Test
    fun `오늘 할 일 탭은 오늘이 대상인 것만 보여 준다`() =
        runTest {
            val viewModel = viewModel(challenges = listOf(myChallenge("ch1")))
            viewModel.onIntent(HomeIntent.Load)

            viewModel.onIntent(HomeIntent.SelectFilter(HomeFilter.TODAY))

            assertEquals(HomeFilter.TODAY, viewModel.uiState.value.filter)
            assertTrue(
                viewModel.uiState.value.visibleChallenges
                    .all { it.todayTarget },
            )
        }

    @Test
    fun `카드를 누르면 그 챌린지 상세로 간다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(challenges = listOf(myChallenge("ch1")), nav = nav)
            viewModel.onIntent(HomeIntent.Load)

            viewModel.onIntent(HomeIntent.OpenChallenge("ch1"))

            assertEquals("ch1", nav.routes.single().args["challengeId"])
        }

    @Test
    fun `탭 이동은 각자 다른 화면으로 간다`() {
        val nav = RecordingNavigationHelper()
        val viewModel = viewModel(nav = nav)

        viewModel.onIntent(HomeIntent.OpenExplore)
        viewModel.onIntent(HomeIntent.OpenMy)
        viewModel.onIntent(HomeIntent.CreateChallenge)

        assertEquals(
            3,
            nav.routes
                .map { it.path }
                .toSet()
                .size,
        )
    }

    private fun viewModel(
        challenges: List<MyChallenge>? = emptyList(),
        progress: ProgressSnapshot? = ProgressSnapshot(asOf = "2026-09-01T00:00:00Z", challenges = emptyList()),
        locals: List<MyChallengeSummary> = emptyList(),
        repo: FakeChallengeRepository =
            FakeChallengeRepository(
                myChallenges = { challenges ?: throw IllegalStateException("목록 조회 실패") },
            ),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = HomeViewModel(
        challengeRepository = repo,
        verificationRepository =
            FakeVerificationRepository(progress = { progress ?: throw IllegalStateException("진행률 조회 실패") }),
        myChallengeStore = FakeMyChallengeStore(locals),
        navigationHelper = nav,
    )

    private fun myChallenge(id: String) =
        MyChallenge(
            challengeId = id,
            title = "아침 6시 기상",
            description = null,
            imageUrl = null,
            category = Category.entries.first(),
            mode = ChallengeMode.SOLO,
            status = ChallengeStatus.ACTIVE,
            participantCount = 1,
            capacity = 1,
            minTier = null,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
            myRole = MemberRole.OWNER,
        )

    private fun local(id: String) =
        MyChallengeSummary(
            challengeId = id,
            title = "방금 만든 챌린지",
            category = Category.entries.first(),
            mode = ChallengeMode.SOLO,
            durationDays = 30,
        )
}

/** 세션 동안만 사는 인메모리 스토어. 홈이 서버보다 먼저 아는 챌린지를 여기서 읽는다. */
private class FakeMyChallengeStore(
    private val items: List<MyChallengeSummary>,
) : MyChallengeStore {
    override fun all(): List<MyChallengeSummary> = items

    override fun add(summary: MyChallengeSummary) = Unit
}
