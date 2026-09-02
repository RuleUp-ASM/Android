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
import com.ruleup.challenge.presentation.detail.fake.FakeReportRepository
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
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportContext
import com.ruleup.report.domain.entity.ReportException
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.report.domain.entity.ReportTarget
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
 * 챌린지 신고. 접수는 되돌릴 수 없고 서버가 결과를 알려주지 않으므로, **잘못 보내지 않는 것**과
 * **보냈는지 확실히 알려주는 것** 두 가지가 이 흐름의 전부다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeDetailReportTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `상세를 못 받았으면 신고를 보내지 않는다`() =
        runTest {
            // 어느 챌린지인지 모르는 상태다 — 보내면 서버가 대상 오류로 튕긴다.
            val reports = FakeReportRepository()
            val model = viewModel(reports = reports)

            model.onIntent(ChallengeDetailIntent.OpenReport)
            model.onIntent(ChallengeDetailIntent.SelectReportReason(ReportReason.SPAM_AD))
            model.onIntent(ChallengeDetailIntent.SubmitReport)

            assertEquals(emptyList(), reports.reported)
        }

    @Test
    fun `사유를 고르지 않으면 신고를 보내지 않는다`() =
        runTest {
            val reports = FakeReportRepository()
            val model = loaded(reports)

            model.onIntent(ChallengeDetailIntent.SubmitReport)

            assertEquals(emptyList(), reports.reported)
        }

    @Test
    fun `고른 사유로 이 챌린지를 상세 화면 맥락에서 신고한다`() =
        runTest {
            val reports = FakeReportRepository()
            val model = loaded(reports)

            model.onIntent(ChallengeDetailIntent.SelectReportReason(ReportReason.INAPPROPRIATE))
            model.onIntent(ChallengeDetailIntent.SubmitReport)

            val target = reports.reported.single() as ReportTarget.Challenge
            assertEquals("ch1", target.challengeId)
            assertEquals(ReportReason.INAPPROPRIATE, target.reason)
            assertEquals(ReportContext.CHALLENGE_DETAIL, target.context)
        }

    @Test
    fun `접수에 성공하면 가림 효과를 담아 완료로 넘어간다`() =
        runTest {
            val reports = FakeReportRepository(result = ReportResult("r-9", HiddenEffect.CHALLENGE_MASKED))
            val model = loaded(reports)

            model.onIntent(ChallengeDetailIntent.SelectReportReason(ReportReason.SPAM_AD))
            model.onIntent(ChallengeDetailIntent.SubmitReport)

            assertEquals(
                "r-9",
                model.uiState.value.reportResult
                    ?.reportId,
            )
            assertEquals(
                HiddenEffect.CHALLENGE_MASKED,
                model.uiState.value.reportResult
                    ?.hiddenEffect,
            )
            assertFalse(model.uiState.value.isSubmittingReport)
        }

    @Test
    fun `두 번 눌러도 한 번만 접수된다`() =
        runTest {
            // 접수는 전건 적재라 두 번 보내면 신고가 두 건 쌓인다.
            val reports = FakeReportRepository()
            val model = loaded(reports)
            model.onIntent(ChallengeDetailIntent.SelectReportReason(ReportReason.SPAM_AD))

            model.onIntent(ChallengeDetailIntent.SubmitReport)
            model.onIntent(ChallengeDetailIntent.SubmitReport)

            assertEquals(1, reports.reported.size)
        }

    @Test
    fun `접수에 실패하면 완료로 넘어가지 않는다`() =
        runTest {
            // 접수가 안 됐는데 완료가 뜨면 사용자는 신고된 줄 안다.
            val reports = FakeReportRepository(error = ReportException(ReportFailure.SUSPENDED, "정지"))
            val model = loaded(reports)

            model.onIntent(ChallengeDetailIntent.SelectReportReason(ReportReason.SPAM_AD))
            model.onIntent(ChallengeDetailIntent.SubmitReport)

            assertNull(model.uiState.value.reportResult)
            assertTrue(model.uiState.value.isReportSheetOpen)
            assertFalse(model.uiState.value.isSubmittingReport)
        }

    @Test
    fun `시트를 닫으면 지난 선택과 접수 결과가 남지 않는다`() =
        runTest {
            // 다음에 열었을 때 지난 사유가 골라져 있으면 실수로 그대로 보낸다.
            val model = loaded(FakeReportRepository())
            model.onIntent(ChallengeDetailIntent.SelectReportReason(ReportReason.SPAM_AD))
            model.onIntent(ChallengeDetailIntent.SubmitReport)

            model.onIntent(ChallengeDetailIntent.DismissReport)

            assertFalse(model.uiState.value.isReportSheetOpen)
            assertNull(model.uiState.value.selectedReportReason)
            assertNull(model.uiState.value.reportResult)
        }

    /** 상세를 받아 두고 신고 시트까지 연 상태. 신고는 이 지점부터만 성립한다. */
    private fun loaded(reports: FakeReportRepository): ChallengeDetailViewModel {
        val model = viewModel(repo = FakeChallengeRepository(detail = { detail() }), reports = reports)
        model.onIntent(ChallengeDetailIntent.Load("ch1"))
        model.onIntent(ChallengeDetailIntent.OpenReport)
        return model
    }

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
            navigationHelper = RecordingNavigationHelper(),
            ttiTracker = TtiTracker(FakeClock(), observability),
        )
    }
}
