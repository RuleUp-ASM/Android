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
import com.ruleup.challenge.presentation.fake.FakeExploreRepository
import com.ruleup.challenge.presentation.fake.FakeRoomRepository
import com.ruleup.challenge.presentation.fake.FakeTargetAppStore
import com.ruleup.challenge.presentation.fake.FakeWatcherRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.api.TtiTracker
import com.ruleup.observability.domain.test.FakeClock
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 챌린지 가입. **거절 사유가 곧 다음 화면을 정한다** — 이미 참여 중이면 알릴 게 없어 조용히
 * 방으로 전환하고, 정원·재입장 대기 같은 사유는 시트로 알린다. 뭉개면 이미 들어와 있는
 * 사용자에게 "참여할 수 없다"는 시트가 뜬다.
 *
 * 이 파일은 가입 경로만 본다 — 상세 화면(1000줄)의 나머지 전이는 대상이 넓어 별도 단위다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailJoinTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `상세를 못 받았으면 가입을 시도하지 않는다`() =
        runTest {
            // 어느 방에 들어갈지 모르는 상태다 — 보내면 서버가 튕긴다.
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
            // 알릴 게 없다 — 시트를 띄우면 들어와 있는 사용자가 "참여할 수 없다"를 본다.
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
            // 정원·자격은 수시로 변한다 — 캐시를 그대로 두면 방금 들어간 방이 여전히 "참여하기"로 보인다.
            val repo =
                repo(join = { JoinResult(countFromCycle = null, requiredPermissions = emptyList(), personalSetupRequired = false) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(ChallengeDetailIntent.Load("ch1"))
            val before = repo.calls.count { it == "getChallenge" }

            viewModel.onIntent(ChallengeDetailIntent.Proceed)

            assertTrue(repo.calls.count { it == "getChallenge" } > before)
        }

    private fun repo(join: (String) -> JoinResult) = FakeChallengeRepository(detail = { detail() }, join = join)

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
            activityRecognition = PermissionState.GRANTED,
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
            navigationHelper = nav,
            ttiTracker = TtiTracker(FakeClock(), observability),
        )
    }
}
