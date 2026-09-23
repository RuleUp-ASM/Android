package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
import com.ruleup.challenge.domain.navigation.MyChallengesPage
import com.ruleup.challenge.presentation.common.SensitiveConsent
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

/**
 * 솔로 방 상세와 탈퇴 목적지.
 *
 * 솔로는 방 홈(`/room`)이 내려오지 않아 **그룹 분기에 얹혀 있던 조회가 통째로 빠진다** — 캘린더가
 * 그 자리였다(APL-08 · APL-09 · VER-01).
 */
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
            // 방 홈은 솔로에 없다 — 부르면 403 을 흡수하느라 잡음만 는다.
            assertTrue("getRoom" !in rooms.calls)
        }

    @Test
    fun `탈퇴하면 뒤로가 아니라 내 챌린지로 스택을 바꾼다`() =
        runTest {
            // 초대 링크로 들어온 경로는 방 상세가 스택의 밑바닥이라 뒤로 보내면 앱이 그대로
            // 종료되고, 초대 화면이 남아 있으면 만료 토큰을 다시 조회해 410 을 본다(ROOM-10 · ROOM-12).
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

    private fun detail(mode: ChallengeMode) =
        ChallengeDetail(
            challengeId = CHALLENGE_ID,
            title = "매일 걷기",
            description = "설명",
            imageUrl = null,
            category = Category.entries.first(),
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
            sensitiveConsent = SensitiveConsent(account, FakeIntroRepository()),
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
