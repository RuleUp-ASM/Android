package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.FakeMyChallengeStore
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
import com.ruleup.challenge.domain.logging.ChallengeEvents
import com.ruleup.challenge.presentation.detail.fake.FakeReportRepository
import com.ruleup.challenge.presentation.fake.FakeAccountRepository
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.FakeRoomRepository
import com.ruleup.challenge.presentation.fake.FakeTargetAppStore
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.remoteconfig.domain.ConfigId
import com.ruleup.remoteconfig.domain.RemoteConfig
import com.ruleup.remoteconfig.domain.RemoteConfigIds
import com.ruleup.remoteconfig.domain.RemoteConfigMetrics
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import com.ruleup.verification.domain.setup.VerificationSetupCompletion
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 그룹 방 진입 시 상세와 방을 함께 그린다 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailRoomLoadTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `그룹 멤버는 방까지 받아 상세와 함께 그린다`() =
        runTest {
            // 상세만 먼저 그리면 참여 전 화면이 잠깐 떴다가 방 화면으로 바뀐다.
            val rooms = FakeRoomRepository(room = { room() })
            val model = viewModel(role = MemberRole.MEMBER, rooms = rooms)

            model.onIntent(ChallengeDetailIntent.Load("ch1"))

            assertNotNull(model.uiState.value.room)
            assertEquals(1, rooms.calls.count { it == "getRoom" })
        }

    @Test
    fun `방을 못 받아도 상세는 그린다`() =
        runTest {
            val model = viewModel(role = MemberRole.MEMBER, rooms = FakeRoomRepository())

            model.onIntent(ChallengeDetailIntent.Load("ch1"))

            assertNotNull(model.uiState.value.detail)
            assertNull(model.uiState.value.room)
        }

    @Test
    fun `준비 직후 방에 들어가면 첫 노출을 new_ready로 기록한다`() =
        runTest {
            val completion = VerificationSetupCompletion().apply { record("ch1") }
            val sources = mutableListOf<String>()
            val metrics =
                object : RemoteConfigMetrics {
                    override suspend fun record(
                        id: ConfigId<*>,
                        event: String,
                        properties: Map<String, String>,
                    ) {
                        if (event == ChallengeEvents.WATCHER_INVITE_ENTRY_SHOWN) sources.add(properties.getValue("entry_source"))
                    }
                }
            val config =
                object : RemoteConfig {
                    override suspend fun <T : Any> get(id: ConfigId<T>): T = id.valueOf("C") ?: id.default

                    override suspend fun onShown(id: ConfigId<*>): Boolean = id == RemoteConfigIds.WatcherInviteUi
                }
            val newRoom = viewModel(MemberRole.OWNER, FakeRoomRepository(), readySetup(), completion, config, metrics)

            newRoom.onIntent(ChallengeDetailIntent.Load("ch1"))
            newRoom.onIntent(ChallengeDetailIntent.ResolveWatcherInvite)
            newRoom.onIntent(ChallengeDetailIntent.WatcherInviteShown)

            assertTrue(newRoom.uiState.value.watcherSetupJustCompleted)
            assertEquals(WatcherInviteSheet.INTRO, newRoom.uiState.value.watcherInviteSheet)
            val reopened = viewModel(MemberRole.OWNER, FakeRoomRepository(), readySetup(), completion, config, metrics)

            reopened.onIntent(ChallengeDetailIntent.Load("ch1"))
            reopened.onIntent(ChallengeDetailIntent.ResolveWatcherInvite)
            reopened.onIntent(ChallengeDetailIntent.WatcherInviteShown)

            assertEquals(listOf("new_ready", "ready_reopen"), sources)
        }

    private fun readySetup() =
        ChallengeSetupInfo(
            manual = false,
            ready = true,
            verificationMethod = VerificationMethod.GPS_PRESENCE,
            requiredPermissions = emptyList(),
            requiresAnchors = true,
            anchorsConfigured = true,
            requiresTargetPackages = false,
        )

    private fun room() =
        ChallengeRoom(
            myRole = MemberRole.MEMBER,
            ownerType = OwnerType.USER,
            summary = RoomSummary(title = "방", roomSuccessRate = null, remainingDays = 3, participantCount = 3, capacity = 4),
            topRanking = emptyList(),
            myTodayStatus = null,
        )

    private fun detail(role: MemberRole) =
        ChallengeDetail(
            title = "아침 6시 기상",
            category = Category.entries.first(),
            imageUrl = null,
            challengeId = "ch1",
            description = null,
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
            myRole = role,
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
        role: MemberRole,
        rooms: FakeRoomRepository,
        setup: ChallengeSetupInfo? = null,
        completion: VerificationSetupCompletion = VerificationSetupCompletion(),
        config: RemoteConfig = RemoteConfig.Defaults,
        metrics: RemoteConfigMetrics = RemoteConfigMetrics.None,
    ): ChallengeDetailViewModel =
        ChallengeDetailViewModel(
            challengeRepository = FakeChallengeRepository(detail = { detail(role) }, setupInfo = setup?.let { { _ -> it } }),
            roomRepository = rooms,
            watcherRepository = FakeWatcherRepository(watchers = { ChallengeWatchers(limit = null, watchers = emptyList()) }),
            verificationRepository = FakeVerificationRepository(),
            permissionStatusProvider = PermissionStatusProvider { snapshot() },
            exploreRepository = FakeExploreRepository(),
            tokenRepository = FakeTokenRepository(storedUserId = "u1"),
            bizLogger = RecordingBizLogger(),
            targetAppStore = FakeTargetAppStore(),
            reportRepository = FakeReportRepository(),
            notificationRepository = FakeNotificationRepository(),
            navigationHelper = RecordingNavigationHelper(),
            checkVerificationAccess = CheckVerificationAccessUseCase(PermissionStatusProvider { snapshot() }, FakeAccountRepository()),
            agreeVerificationConsent = AgreeVerificationConsentUseCase(FakeAccountRepository(), FakeIntroRepository()),
            myChallengeStore = FakeMyChallengeStore(),
            remoteConfig = config,
            remoteConfigMetrics = metrics,
            setupCompletion = completion,
        )
}
