package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
import com.ruleup.challenge.presentation.detail.fake.FakeReportRepository
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.FakeRoomRepository
import com.ruleup.challenge.presentation.fake.FakeTargetAppStore
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.notification.domain.entity.NotificationGroupSettings
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.observability.domain.test.testObservability
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 방별 알림 음소거 — 알림 3계층의 ③.
 *
 * 이 토글이 지키는 건 **틀린 상태를 그리지 않는 것**이다. 껐다고 믿은 방에서 푸시가 계속 오면
 * 사용자는 원인을 찾을 길이 없고, 결국 앱 전체 알림을 차단해 강퇴·잠금 고지까지 잃는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailMuteTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `상세를 열면 이 방이 음소거인지 함께 읽는다`() =
        runTest {
            val viewModel = viewModel(notifications = repo(settings(muted = listOf("ch1"))))

            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            assertEquals(true, viewModel.uiState.value.isMuted)
        }

    @Test
    fun `음소거가 아니면 꺼진 상태로 그린다`() =
        runTest {
            val viewModel = viewModel(notifications = repo(settings(muted = emptyList())))

            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            assertEquals(false, viewModel.uiState.value.isMuted)
        }

    @Test
    fun `설정을 못 읽으면 토글을 그리지 않는다`() =
        runTest {
            // 모르는 상태를 「켜짐」으로 그리면 껐다고 믿은 방에서 푸시가 계속 온다.
            val viewModel = viewModel(notifications = FakeNotificationRepository())

            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            assertNull(viewModel.uiState.value.isMuted)
        }

    @Test
    fun `토글을 켜면 서버에 음소거를 등록한다`() =
        runTest {
            val notifications = repo(settings(muted = emptyList()))
            val viewModel = viewModel(notifications = notifications)
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            viewModel.onIntent(ChallengeDetailIntent.ToggleMute(true))

            assertEquals(listOf("ch1" to true), notifications.mutes)
            assertEquals(true, viewModel.uiState.value.isMuted)
        }

    @Test
    fun `서버가 거절하면 화면 상태를 바꾸지 않는다`() =
        runTest {
            // 화면만 꺼진 것처럼 보이면 사용자가 원인을 찾을 길이 없다.
            val notifications =
                repo(settings(muted = emptyList()), mute = { _, _ -> throw IllegalStateException("서버 오류") })
            val viewModel = viewModel(notifications = notifications)
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            viewModel.onIntent(ChallengeDetailIntent.ToggleMute(true))

            assertEquals(false, viewModel.uiState.value.isMuted)
            assertFalse(viewModel.uiState.value.isMuteSubmitting)
        }

    @Test
    fun `상세를 아직 못 받았으면 토글해도 보내지 않는다`() =
        runTest {
            // 어느 방을 끌지 모르는 상태다 — 보내면 엉뚱한 방이 조용해진다.
            val notifications = repo(settings(muted = emptyList()))
            val viewModel = viewModel(notifications = notifications)

            viewModel.onIntent(ChallengeDetailIntent.ToggleMute(true))

            assertTrue(notifications.mutes.isEmpty())
        }

    private fun repo(
        settings: NotificationSettings,
        mute: ((String, Boolean) -> Unit)? = null,
    ) = FakeNotificationRepository(settings = { settings }, mute = mute)

    private fun settings(muted: List<String>) =
        NotificationSettings(
            pushEnabled = true,
            groups = NotificationGroupSettings(account = true, challenge = true, marketing = true),
            mutedChallengeIds = muted,
        )

    private fun detail() =
        ChallengeDetail(
            challengeId = "ch1",
            title = "아침 6시 기상",
            description = null,
            imageUrl = null,
            category = Category.entries.first(),
            mode = ChallengeMode.GROUP,
            visibility = ChallengeVisibility.PUBLIC,
            status = ChallengeStatus.ACTIVE,
            owner = null,
            ownerType = OwnerType.USER,
            participantCount = 3,
            capacity = 4,
            isFull = false,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
            verification =
                VerificationConfig(
                    type = VerificationType.entries.first(),
                    method = VerificationMethod.entries.first(),
                ),
            stats = ChallengeStats(completionRate = null, retentionRate = null),
            gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
            joinBlockReason = null,
            rejoinAvailableAt = null,
            joinNote = JoinNote.IMMEDIATE,
            cloneable = false,
            myRole = MemberRole.NONE,
            moderation = null,
        )

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

    private fun viewModel(
        notifications: FakeNotificationRepository = FakeNotificationRepository(),
        repo: FakeChallengeRepository = FakeChallengeRepository(detail = { detail() }),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        reports: FakeReportRepository = FakeReportRepository(),
    ): ChallengeDetailViewModel {
        val observability = testObservability()
        return ChallengeDetailViewModel(
            challengeRepository = repo,
            roomRepository = FakeRoomRepository(),
            watcherRepository = FakeWatcherRepository(),
            verificationRepository = FakeVerificationRepository(),
            permissionStatusProvider = PermissionStatusProvider { snapshot() },
            exploreRepository = FakeExploreRepository(),
            tokenRepository = FakeTokenRepository(storedUserId = "u1"),
            observability = observability,
            targetAppStore = FakeTargetAppStore(),
            reportRepository = reports,
            notificationRepository = notifications,
            navigationHelper = nav,
        )
    }
}
