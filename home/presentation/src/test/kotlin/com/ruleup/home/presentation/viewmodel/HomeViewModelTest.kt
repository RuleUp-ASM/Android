package com.ruleup.home.presentation.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengePage
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.domain.time.ServiceDate
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.TodayStatus
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

/** 홈. */
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
            val repo = FakeChallengeRepository(myChallenges = { _, _ -> page(myChallenge("ch1")) })
            val viewModel = viewModel(repo = repo)

            viewModel.onIntent(HomeIntent.Load)
            viewModel.onIntent(HomeIntent.Load)

            assertEquals(2, repo.calls.count { it == "getMyChallenges" })
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

    @Test
    fun `이번 주 판정만 골라 담는다`() =
        runTest {
            val today = ServiceDate.today()
            val lastMonth = today.minusMonths(1).toString()
            val myPage =
                FakeMyPageRepository { month ->
                    ActivityCalendar(
                        month = month,
                        days =
                            listOf(
                                CalendarDay(today.toString(), CalendarDayStatus.ALL_DONE, successCount = 1, targetCount = 1),
                                CalendarDay(lastMonth, CalendarDayStatus.FAILED, successCount = 0, targetCount = 1),
                                CalendarDay(today.minusDays(today.dayOfWeek.value.toLong()).toString(), null, 0, 0),
                            ),
                    )
                }
            val vm = viewModel(myPage = myPage)

            vm.onIntent(HomeIntent.Load)

            assertEquals(mapOf(today.toString() to CalendarDayStatus.ALL_DONE), vm.uiState.value.weekStatuses)
        }

    @Test
    fun `캘린더 조회가 실패해도 홈은 뜨고 이번 주는 비어 있다`() =
        runTest {
            val vm = viewModel(myPage = FakeMyPageRepository { error("캘린더 실패") })

            vm.onIntent(HomeIntent.Load)

            assertFalse(vm.uiState.value.isLoading)
            assertEquals(emptyMap(), vm.uiState.value.weekStatuses)
        }

    @Test
    fun `오늘 해 볼 후보는 상세로 인증 방식을 확인해 직접 체크인 것을 올린다`() =
        runTest {
            val repo =
                FakeChallengeRepository(
                    myChallenges = { _, _ -> page(myChallenge("manual"), myChallenge("auto")) },
                    detail = { id -> detail(id, if (id == "manual") VerificationType.MANUAL else VerificationType.AUTO) },
                )
            val vm = viewModel(repo = repo, progress = snapshot(todayProgress("manual"), todayProgress("auto")))

            vm.onIntent(HomeIntent.Load)

            assertEquals(
                "manual",
                vm.uiState.value.hero
                    ?.challengeId,
            )
        }

    @Test
    fun `인증 방식을 한 번 받은 챌린지는 다시 불러와도 묻지 않는다`() =
        runTest {
            val repo =
                FakeChallengeRepository(
                    myChallenges = { _, _ -> page(myChallenge("ch1")) },
                    detail = { id -> detail(id, VerificationType.MANUAL) },
                )
            val vm = viewModel(repo = repo, progress = snapshot(todayProgress("ch1")))

            vm.onIntent(HomeIntent.Load)
            vm.onIntent(HomeIntent.Load)

            assertEquals(1, repo.calls.count { it == "getChallenge" })
        }

    @Test
    fun `상세 조회가 실패해도 홈은 뜨고 오늘 해 볼 것만 빠진다`() =
        runTest {
            val repo =
                FakeChallengeRepository(
                    myChallenges = { _, _ -> page(myChallenge("ch1")) },
                    detail = { error("상세 실패") },
                )
            val vm = viewModel(repo = repo, progress = snapshot(todayProgress("ch1")))

            vm.onIntent(HomeIntent.Load)

            assertEquals(
                listOf("ch1"),
                vm.uiState.value.challenges
                    .map { it.challengeId },
            )
            assertNull(vm.uiState.value.hero)
        }

    private fun viewModel(
        challenges: List<MyChallenge>? = emptyList(),
        progress: ProgressSnapshot? = ProgressSnapshot(asOf = "2026-09-01T00:00:00Z", challenges = emptyList()),
        locals: List<MyChallengeSummary> = emptyList(),
        repo: FakeChallengeRepository =
            FakeChallengeRepository(
                myChallenges = { _, _ -> page(*(challenges ?: throw IllegalStateException("목록 조회 실패")).toTypedArray()) },
            ),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        myPage: FakeMyPageRepository = FakeMyPageRepository(),
    ) = HomeViewModel(
        challengeRepository = repo,
        verificationRepository =
            FakeVerificationRepository(progress = { progress ?: throw IllegalStateException("진행률 조회 실패") }),
        myChallengeStore = FakeMyChallengeStore(locals),
        notificationRepository = FakeNotificationRepository(),
        myPageRepository = myPage,
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
            visibility = null,
            participantCount = 1,
            capacity = 1,
            minTier = null,
            weeklyCount = 7,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
            myRole = MemberRole.OWNER,
            ownerType = OwnerType.USER,
            leftType = null,
            leftAt = null,
            successRate = null,
        )

    private fun snapshot(vararg items: ChallengeProgress) = ProgressSnapshot(asOf = "2026-09-01T00:00:00Z", challenges = items.toList())

    private fun todayProgress(id: String) =
        ChallengeProgress(
            challengeId = id,
            title = "아침 6시 기상",
            category = Category.entries.first(),
            participationType = ChallengeMode.SOLO.value,
            status = ChallengeStatus.ACTIVE.value,
            progressRate = 40.0,
            successDays = 4,
            targetDays = 30,
            remainingDays = 26,
            todayTarget = true,
            todayStatus = TodayStatus.IN_PROGRESS,
            lastSyncedAt = null,
        )

    /** 홈이 상세에서 보는 건 「오늘 직접 체크할 수 있는가」뿐이다. */
    private fun detail(
        id: String,
        type: VerificationType,
    ) = ChallengeDetail(
        title = "아침 6시 기상",
        category = Category.entries.first(),
        imageUrl = null,
        challengeId = id,
        description = null,
        mode = ChallengeMode.SOLO,
        visibility = null,
        status = ChallengeStatus.ACTIVE,
        owner = null,
        ownerType = OwnerType.USER,
        participantCount = 1,
        capacity = 1,
        isFull = false,
        period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
        verification = VerificationConfig(type = type, method = VerificationMethod.entries.first()),
        stats = ChallengeStats(completionRate = null, retentionRate = null),
        gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
        joinBlockReason = null,
        rejoinAvailableAt = null,
        joinNote = JoinNote.IMMEDIATE,
        cloneable = false,
        myRole = MemberRole.OWNER,
        moderation = null,
    )

    /** 홈은 첫 페이지만 본다 */
    private fun page(vararg challenges: MyChallenge) = MyChallengePage(challenges = challenges.toList(), nextCursor = null, hasNext = false)

    private fun local(id: String) =
        MyChallengeSummary(
            challengeId = id,
            title = "방금 만든 챌린지",
            category = Category.entries.first(),
            mode = ChallengeMode.SOLO,
            durationDays = 30,
        )
}

/** 세션 동안만 사는 인메모리 스토어. */
private class FakeMyChallengeStore(
    private val items: List<MyChallengeSummary>,
) : MyChallengeStore {
    override fun all(): List<MyChallengeSummary> = items

    override fun add(summary: MyChallengeSummary) = Unit
}
