package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.FakeMyChallengeStore
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
import com.ruleup.challenge.domain.navigation.MyChallengesPage
import com.ruleup.challenge.presentation.detail.fake.FakeReportRepository
import com.ruleup.challenge.presentation.fake.FakeAccountRepository
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.FakeRoomRepository
import com.ruleup.challenge.presentation.fake.FakeTargetAppStore
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import com.ruleup.verification.domain.test.FakeVerificationRepository
import com.ruleup.verification.domain.usecase.AgreeVerificationConsentUseCase
import com.ruleup.verification.domain.usecase.CheckVerificationAccessUseCase
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
import kotlin.test.assertTrue

/** 솔로 방 상세와 탈퇴 목적지. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailSoloTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `솔로 방은 방 홈 없이도 캘린더를 받는다`() =
        runTest {
            val rooms =
                FakeRoomRepository(calendar = {
                    id,
                    month,
                    ->
                    ChallengeCalendar(challengeId = id, month = month, days = emptyList())
                })

            viewModel(
                repo = FakeChallengeRepository(detail = { detail(mode = ChallengeMode.SOLO) }),
                rooms = rooms,
            ).onIntent(ChallengeDetailIntent.Load(CHALLENGE_ID))

            assertTrue("getCalendar" in rooms.calls)
            // 방 홈은 솔로에 없다
            assertTrue("getRoom" !in rooms.calls)
        }

    @Test
    fun `솔로 방은 내 피드를 받고 랭킹 범위를 챌린지 순위로 둔다`() =
        runTest {
            // 솔로에는 방 안 순위가 없어 멤버 순위로 두면 랭킹 탭이 빈다(#585).
            val rooms =
                FakeRoomRepository(
                    calendar = { id, month -> ChallengeCalendar(challengeId = id, month = month, days = emptyList()) },
                    threads = { _, _ -> ChallengeThreads(items = emptyList(), nextCursor = null) },
                )
            val viewModel =
                viewModel(repo = FakeChallengeRepository(detail = { detail(mode = ChallengeMode.SOLO) }), rooms = rooms)

            viewModel.onIntent(ChallengeDetailIntent.Load(CHALLENGE_ID))

            assertTrue("getThreads" in rooms.calls)
            assertEquals(RankingScope.ROOM, viewModel.uiState.value.rankingScope)
        }

    @Test
    fun `그룹 방 멤버도 내 성공 실패 캘린더를 받는다`() =
        runTest {
            // 그룹에서 건너뛰면 ⋯ → 캘린더가 빈 채로 열린다(#572).
            val rooms =
                FakeRoomRepository(calendar = { id, month -> ChallengeCalendar(challengeId = id, month = month, days = emptyList()) })

            viewModel(
                repo = FakeChallengeRepository(detail = { detail(mode = ChallengeMode.GROUP) }),
                rooms = rooms,
            ).onIntent(ChallengeDetailIntent.Load(CHALLENGE_ID))

            assertTrue("getCalendar" in rooms.calls)
        }

    @Test
    fun `탈퇴하면 뒤로가 아니라 내 챌린지로 스택을 바꾼다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(
                    repo =
                        FakeChallengeRepository(
                            detail = { detail(mode = ChallengeMode.SOLO) },
                            leave = { LeaveResult(penaltyApplied = false) },
                        ),
                    nav = nav,
                )
            viewModel.onIntent(ChallengeDetailIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ChallengeDetailIntent.LeaveChallenge)

            assertEquals(0, nav.backCount)
            assertEquals(MyChallengesPage.PATH, nav.replaced.single().path)
        }

    @Test
    fun `탈퇴하면 방금 만든 챌린지 기록에서도 빠진다`() =
        runTest {
            // 남아 있으면 홈 목록 조회가 실패할 때 나간 방이 다시 그려진다.
            val store = FakeMyChallengeStore(listOf(summary(CHALLENGE_ID), summary("other")))
            val viewModel =
                viewModel(
                    repo =
                        FakeChallengeRepository(
                            detail = { detail(mode = ChallengeMode.SOLO) },
                            leave = { LeaveResult(penaltyApplied = false) },
                        ),
                    store = store,
                )
            viewModel.onIntent(ChallengeDetailIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ChallengeDetailIntent.LeaveChallenge)

            assertEquals(listOf("other"), store.all().map { it.challengeId })
        }

    @Test
    fun `탈퇴에 실패하면 방금 만든 챌린지 기록을 지우지 않는다`() =
        runTest {
            val store = FakeMyChallengeStore(listOf(summary(CHALLENGE_ID)))
            val viewModel =
                viewModel(
                    repo =
                        FakeChallengeRepository(
                            detail = { detail(mode = ChallengeMode.SOLO) },
                            leave = { throw IllegalStateException("탈퇴 실패") },
                        ),
                    store = store,
                )
            viewModel.onIntent(ChallengeDetailIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ChallengeDetailIntent.LeaveChallenge)

            assertEquals(listOf(CHALLENGE_ID), store.all().map { it.challengeId })
        }

    private fun summary(id: String) =
        MyChallengeSummary(
            challengeId = id,
            title = "방금 만든 챌린지",
            category = Category.entries.first(),
            mode = ChallengeMode.SOLO,
            durationDays = 30,
        )

    private fun detail(mode: ChallengeMode) =
        ChallengeDetail(
            title = "매일 걷기",
            category = Category.entries.first(),
            imageUrl = null,
            challengeId = CHALLENGE_ID,
            description = "설명",
            mode = mode,
            visibility = null,
            status = ChallengeStatus.ACTIVE,
            owner = null,
            ownerType = OwnerType.USER,
            participantCount = 1,
            capacity = 1,
            isFull = false,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-09-30"),
            verification = VerificationConfig(type = VerificationType.MANUAL, method = VerificationMethod.SELF_CHECK),
            stats = ChallengeStats(completionRate = null, retentionRate = null),
            gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
            joinBlockReason = null,
            rejoinAvailableAt = null,
            joinNote = JoinNote.IMMEDIATE,
            cloneable = false,
            myRole = MemberRole.MEMBER,
            moderation = null,
        )

    private fun viewModel(
        repo: FakeChallengeRepository,
        rooms: FakeRoomRepository = FakeRoomRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        store: FakeMyChallengeStore = FakeMyChallengeStore(),
    ): ChallengeDetailViewModel {
        val account = FakeAccountRepository(agreed = AgreementType.entries.toSet())
        return ChallengeDetailViewModel(
            challengeRepository = repo,
            roomRepository = rooms,
            watcherRepository = FakeWatcherRepository(),
            verificationRepository = FakeVerificationRepository(),
            permissionStatusProvider = PermissionStatusProvider { snapshot() },
            exploreRepository = FakeExploreRepository(),
            tokenRepository = FakeTokenRepository(storedUserId = "u1"),
            bizLogger = RecordingBizLogger(),
            targetAppStore = FakeTargetAppStore(),
            reportRepository = FakeReportRepository(),
            notificationRepository = FakeNotificationRepository(),
            navigationHelper = nav,
            checkVerificationAccess = CheckVerificationAccessUseCase(PermissionStatusProvider { snapshot() }, account),
            agreeVerificationConsent = AgreeVerificationConsentUseCase(account, FakeIntroRepository()),
            myChallengeStore = store,
        )
    }

    private fun snapshot() =
        PermissionSnapshot(
            location = PermissionState.GRANTED,
            backgroundLocation = PermissionState.GRANTED,
            usageStats = PermissionState.GRANTED,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )

    private companion object {
        const val CHALLENGE_ID = "ch1"
    }
}
