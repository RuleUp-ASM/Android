package com.ruleup.challenge.presentation.detail.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 챌린지 가입. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailJoinTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `상세를 못 받았으면 가입을 시도하지 않는다`() =
        runTest {
            // 어느 방에 들어갈지 모르는 상태다
            val repo = FakeChallengeRepository()
            val viewModel = viewModel(repo)

            viewModel.onIntent(ChallengeDetailIntent.Proceed)

            assertTrue(repo.calls.none { it == "join" })
        }

    @Test
    fun `정원이 찼으면 그 사유로 차단 시트를 띄운다`() =
        runTest {
            val viewModel = viewModel(repo(join = { throw JoinBlockedException(JoinBlockReason.FULL) }))
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            viewModel.onIntent(ChallengeDetailIntent.Proceed)

            assertEquals(
                JoinBlockReason.FULL,
                viewModel.uiState.value.joinBlock
                    ?.reason,
            )
        }

    @Test
    fun `재입장 대기는 언제부터 가능한지 함께 싣는다`() =
        runTest {
            val viewModel =
                viewModel(
                    repo(
                        join = {
                            throw JoinBlockedException(
                                reason = JoinBlockReason.REJOIN_COOLDOWN,
                                rejoinAvailableAt = "2026-09-08T00:00:00Z",
                            )
                        },
                    ),
                )
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            viewModel.onIntent(ChallengeDetailIntent.Proceed)

            assertEquals(
                "2026-09-08T00:00:00Z",
                viewModel.uiState.value.joinBlock
                    ?.rejoinAvailableAt,
            )
        }

    @Test
    fun `이미 참여 중이면 차단 시트를 띄우지 않는다`() =
        runTest {
            // 알릴 게 없다
            val viewModel = viewModel(repo(join = { throw JoinBlockedException(JoinBlockReason.ALREADY_JOINED) }))
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))

            viewModel.onIntent(ChallengeDetailIntent.Proceed)

            assertNull(viewModel.uiState.value.joinBlock)
        }

    @Test
    fun `차단 시트를 닫으면 상태에서 지운다`() =
        runTest {
            val viewModel = viewModel(repo(join = { throw JoinBlockedException(JoinBlockReason.FULL) }))
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))
            viewModel.onIntent(ChallengeDetailIntent.Proceed)
            assertNotNull(viewModel.uiState.value.joinBlock)

            viewModel.onIntent(ChallengeDetailIntent.DismissJoinBlock)

            assertNull(viewModel.uiState.value.joinBlock)
        }

    @Test
    fun `가입에 성공하면 상세를 다시 받는다`() =
        runTest {
            val repo =
                repo(join = { JoinResult(countFromCycle = null, requiredPermissions = emptyList(), personalSetupRequired = false) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))
            val before = repo.calls.count { it == "getChallenge" }

            viewModel.onIntent(ChallengeDetailIntent.Proceed)

            assertTrue(repo.calls.count { it == "getChallenge" } > before)
        }

    @Test
    fun `위치 인증 방은 개별 동의를 받기 전에 가입하지 않고 동의 시트를 띄운다`() =
        runTest {
            // 첫 사용 시점을 놓치면 위치정보 개별 동의를 끝내 받지 못한다(ONB-14).
            val repo = repo(join = { JoinResult(countFromCycle = null, requiredPermissions = emptyList(), personalSetupRequired = false) })
            val model = viewModel(repo = repo, account = FakeAccountRepository())
            model.onIntent(ChallengeDetailIntent.Load("ch1"))

            model.onIntent(ChallengeDetailIntent.Proceed)

            assertTrue(repo.calls.none { it == "join" })
            assertEquals(
                listOf(AgreementType.LOCATION_INFO),
                model.uiState.value.pendingAccess
                    ?.missingConsents,
            )
        }

    @Test
    fun `개별 동의에 동의하면 기록한 뒤 이어서 가입한다`() =
        runTest {
            val repo = repo(join = { JoinResult(countFromCycle = null, requiredPermissions = emptyList(), personalSetupRequired = false) })
            val account = FakeAccountRepository()
            val model = viewModel(repo = repo, account = account)
            model.onIntent(ChallengeDetailIntent.Load("ch1"))
            model.onIntent(ChallengeDetailIntent.Proceed)

            model.onIntent(ChallengeDetailIntent.ConfirmVerificationAccess)

            assertEquals(AgreementType.LOCATION_INFO, account.submitted.single().type)
            assertTrue(repo.calls.any { it == "join" })
        }

    @Test
    fun `위치와 건강 권한이 함께 필요하면 두 동의를 받은 뒤 참여한다`() =
        runTest {
            val tokens = listOf("ACCESS_FINE_LOCATION", "READ_SLEEP")
            val repo = repo(requiredPermissions = tokens, join = { JoinResult(null, emptyList(), false) })
            val account = FakeAccountRepository()
            val model = viewModel(repo = repo, account = account)
            model.onIntent(ChallengeDetailIntent.Load("ch1"))
            model.onIntent(ChallengeDetailIntent.Proceed)

            model.onIntent(ChallengeDetailIntent.ConfirmVerificationAccess)
            assertEquals(listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO), account.submitted.map { it.type })
            assertEquals(1, repo.calls.count { it == "join" })
        }

    @Test
    fun `동의 조회가 실패하면 참여하지 않는다`() =
        runTest {
            val repo = repo(join = { JoinResult(null, emptyList(), false) })
            val model = viewModel(repo = repo, account = FakeAccountRepository(readError = IllegalStateException("조회 실패")))
            model.onIntent(ChallengeDetailIntent.Load("ch1"))

            model.onIntent(ChallengeDetailIntent.Proceed)

            assertTrue("join" !in repo.calls)
            assertNull(model.uiState.value.pendingAccess)
        }

    @Test
    fun `서버 동의를 받았어도 기기 권한이 없으면 참여하지 않고 권한 상태를 갱신한다`() =
        runTest {
            val repo = repo(join = { JoinResult(null, emptyList(), false) })
            val model = viewModel(repo = repo, permissions = snapshot().copy(location = PermissionState.DENIED))
            model.onIntent(ChallengeDetailIntent.Load("ch1"))

            model.onIntent(ChallengeDetailIntent.Proceed)

            assertTrue("join" !in repo.calls)
            assertEquals(
                PermissionState.DENIED,
                model.uiState.value.permissions
                    ?.location,
            )
        }

    @Test
    fun `권한 설정에서 돌아오면 같은 설정을 갱신하고 준비된 경우에만 참여한다`() =
        runTest {
            val repo = repo(join = { JoinResult(null, emptyList(), false) })
            val account = FakeAccountRepository()
            var permissions = snapshot().copy(location = PermissionState.DENIED)
            val model = viewModel(repo = repo, account = account, permissionStatus = PermissionStatusProvider { permissions })
            model.onIntent(ChallengeDetailIntent.Load("ch1"))
            model.onIntent(ChallengeDetailIntent.Proceed)
            model.onIntent(ChallengeDetailIntent.ConfirmVerificationAccess)

            model.onIntent(ChallengeDetailIntent.VerificationPermissionsReturned)
            assertTrue("join" !in repo.calls)
            assertEquals(
                listOf("ACCESS_FINE_LOCATION"),
                model.uiState.value.pendingAccess
                    ?.missingPermissions,
            )

            permissions = snapshot()
            model.onIntent(ChallengeDetailIntent.VerificationPermissionsReturned)
            model.onIntent(ChallengeDetailIntent.VerificationPermissionsReturned)
            assertNull(model.uiState.value.pendingAccess)
            assertEquals(1, repo.calls.count { it == "join" })
            assertEquals(1, account.submissionBatches.size)
        }

    @Test
    fun `권한 허용하기로 연 설정을 마쳐도 장소와 앱 등록을 건너뛰어 참여하지 않는다`() =
        runTest {
            val repo = repo(join = { JoinResult(null, emptyList(), false) })
            var permissions = snapshot().copy(location = PermissionState.DENIED)
            val model = viewModel(repo = repo, permissionStatus = PermissionStatusProvider { permissions })
            model.onIntent(ChallengeDetailIntent.Load("ch1"))
            model.onIntent(ChallengeDetailIntent.OpenVerificationAccess)
            model.onIntent(ChallengeDetailIntent.ConfirmVerificationAccess)

            permissions = snapshot()
            model.onIntent(ChallengeDetailIntent.VerificationPermissionsReturned)

            assertNull(model.uiState.value.pendingAccess)
            assertTrue("join" !in repo.calls)
        }

    @Test
    fun `참여 설정을 닫으면 늦게 도착한 권한 결과로 참여하지 않는다`() =
        runTest {
            val repo = repo(join = { JoinResult(null, emptyList(), false) })
            val model = viewModel(repo = repo, account = FakeAccountRepository())
            model.onIntent(ChallengeDetailIntent.Load("ch1"))
            model.onIntent(ChallengeDetailIntent.Proceed)

            model.onIntent(ChallengeDetailIntent.DismissVerificationAccess)
            model.onIntent(ChallengeDetailIntent.VerificationPermissionsReturned)

            assertNull(model.uiState.value.pendingAccess)
            assertTrue("join" !in repo.calls)
        }

    private fun repo(
        requiredPermissions: List<String> = listOf("ACCESS_FINE_LOCATION"),
        join: (String) -> JoinResult,
    ) = FakeChallengeRepository(detail = { detail(requiredPermissions) }, join = join)

    private fun detail(requiredPermissions: List<String>) =
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
                    method = VerificationMethod.SELF_CHECK,
                    requiredPermissions = requiredPermissions,
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
        repo: FakeChallengeRepository = FakeChallengeRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        reports: FakeReportRepository = FakeReportRepository(),
        account: FakeAccountRepository = FakeAccountRepository(agreed = AgreementType.entries.toSet()),
        permissions: PermissionSnapshot = snapshot(),
        permissionStatus: PermissionStatusProvider = PermissionStatusProvider { permissions },
    ): ChallengeDetailViewModel {
        val bizLogger = RecordingBizLogger()
        return ChallengeDetailViewModel(
            challengeRepository = repo,
            roomRepository = FakeRoomRepository(),
            watcherRepository = FakeWatcherRepository(),
            verificationRepository = FakeVerificationRepository(),
            permissionStatusProvider = permissionStatus,
            exploreRepository = FakeExploreRepository(),
            tokenRepository = FakeTokenRepository(storedUserId = "u1"),
            bizLogger = bizLogger,
            targetAppStore = FakeTargetAppStore(),
            reportRepository = reports,
            notificationRepository = FakeNotificationRepository(),
            navigationHelper = nav,
            checkVerificationAccess = CheckVerificationAccessUseCase(permissionStatus, account),
            agreeVerificationConsent = AgreeVerificationConsentUseCase(account, FakeIntroRepository()),
        )
    }
}
