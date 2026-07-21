package com.ruleup.verification.domain.usecase

import com.ruleup.analytics.domain.AnalyticsEvent
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceClock
import com.ruleup.verification.domain.entity.DeviceDiagnostics
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.IntegritySnapshot
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.LocationPoint
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.NetworkState
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.entity.VerificationSignal
import com.ruleup.verification.domain.repository.EnvelopeMetadataProvider
import com.ruleup.verification.domain.repository.SignalCollector
import com.ruleup.verification.domain.repository.SignalRepository
import com.ruleup.verification.domain.repository.VerificationRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunSyncUseCaseTest {
    private val scope = SignalScope(targetPackages = emptySet(), activeRequestIds = setOf("m1"))
    private val collectedAt = "2026-06-21T00:00:00Z"

    @Test
    fun `활성 챌린지도 보낼 것도 없으면 전송하지 않고 null 을 반환한다`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = null)
            val verificationRepo = FakeVerificationRepository()
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(),
                    verificationRepo,
                    FakeAnalyticsLogger(),
                )

            val result = useCase(scope, collectedAt)

            assertNull(result)
            assertFalse(verificationRepo.syncCalled)
            assertFalse(signalRepo.markSyncedCalled)
        }

    @Test
    fun `활성 챌린지가 있으면 신호·gap 이 없어도 빈 envelope 를 전송한다`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = null)
            val verificationRepo = FakeVerificationRepository(result = syncResult())
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(activeChallengeIds = listOf("c1")),
                    verificationRepo,
                    FakeAnalyticsLogger(),
                )

            val result = useCase(scope, collectedAt)

            assertTrue(verificationRepo.syncCalled)
            assertTrue(verificationRepo.syncedBatch?.isEmpty == true)
            assertEquals(1, result?.updatedChallenges?.size)
        }

    @Test
    fun `성공하면 markSynced 후 결과를 반환한다 - 누적분 전송`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = nonEmptyBatch())
            val verificationRepo = FakeVerificationRepository(result = syncResult())
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(),
                    verificationRepo,
                    FakeAnalyticsLogger(),
                )

            val result = useCase(scope, collectedAt)

            assertEquals(1, result?.updatedChallenges?.size)
            assertTrue(verificationRepo.syncCalled)
            assertTrue(signalRepo.markSyncedCalled)
            assertTrue(signalRepo.purgeCalled)
        }

    @Test
    fun `400 INVALID_SIGNAL_PAYLOAD 은 배치를 폐기(markSynced)하고 예외 전파한다`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = nonEmptyBatch())
            val verificationRepo = FakeVerificationRepository(error = InvalidSignalPayloadException())
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(),
                    verificationRepo,
                    FakeAnalyticsLogger(),
                )

            assertFailsWith<InvalidSignalPayloadException> { useCase(scope, collectedAt) }
            // 폐기 = synced 표시(무한 재전송 금지).
            assertTrue(signalRepo.markSyncedCalled)
        }

    @Test
    fun `429 SYNC_TOO_FREQUENT 은 markSynced 없이 예외 전파한다 - 백오프 재시도용`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = nonEmptyBatch())
            val verificationRepo = FakeVerificationRepository(error = SyncTooFrequentException())
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(),
                    verificationRepo,
                    FakeAnalyticsLogger(),
                )

            assertFailsWith<SyncTooFrequentException> { useCase(scope, collectedAt) }
            // 전송분 유지(다음 백오프 재시도가 다시 보냄).
            assertFalse(signalRepo.markSyncedCalled)
        }

    private fun nonEmptyBatch(): SignalBatch =
        SignalBatch(
            collectedAt = collectedAt,
            signals = listOf(VerificationSignal.Locations(listOf(LocationPoint(37.0, 127.0, 5f, false, 1L)))),
        )

    private fun syncResult(): SyncResult =
        SyncResult(
            syncedAt = collectedAt,
            nextSyncAfterSec = 1800,
            updatedChallenges =
                listOf(
                    com.ruleup.verification.domain.entity.UpdatedChallenge(
                        "c1",
                        com.ruleup.verification.domain.entity.TodayStatus.SUCCESS,
                        50.0,
                    ),
                ),
            ignoredSignalTypes = emptyList(),
        )

    private class FakeSignalCollector : SignalCollector {
        override suspend fun capture(scope: SignalScope) = Unit
    }

    private class FakeSignalRepository(
        private val drain: SignalBatch?,
        private val gaps: List<SignalGap> = emptyList(),
    ) : SignalRepository {
        var markSyncedCalled = false
        var purgeCalled = false

        override suspend fun drainPending(collectedAt: String): SignalBatch? = drain

        override suspend fun drainGaps(collectedAt: String): List<SignalGap> = gaps

        override suspend fun markSynced(collectedAt: String) {
            markSyncedCalled = true
        }

        override suspend fun purgeExpired(ttlMillis: Long) {
            purgeCalled = true
        }
    }

    private class FakeEnvelopeMetadataProvider(
        private val activeChallengeIds: List<String> = emptyList(),
    ) : EnvelopeMetadataProvider {
        override suspend fun capture(scope: SignalScope): EnvelopeMetadata =
            EnvelopeMetadata(
                clock = DeviceClock(deviceTimeMillis = 1L, elapsedRealtimeMillis = 1L, bootSessionId = "boot", timeZone = "Asia/Seoul"),
                activeChallengeIds = activeChallengeIds,
                permissions =
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
                    ),
                network = NetworkState(vpnActive = false),
                integrity = IntegritySnapshot(token = null),
                diagnostics = DeviceDiagnostics(null, null, null, null, null, null, null),
                gaps = emptyList(),
            )
    }

    private class FakeVerificationRepository(
        private val result: SyncResult? = null,
        private val error: Throwable? = null,
    ) : VerificationRepository {
        var syncCalled = false
        var syncedBatch: SignalBatch? = null

        override suspend fun submitIntro(intro: DeviceIntro): SyncPolicy = error("unused")

        override suspend fun sync(
            metadata: EnvelopeMetadata,
            batch: SignalBatch,
        ): SyncResult {
            syncCalled = true
            syncedBatch = batch
            error?.let { throw it }
            return requireNotNull(result)
        }

        override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot = error("unused")

        override suspend fun getVerificationDetail(
            challengeId: String,
            logDays: Int,
        ): VerificationDetail = error("unused")

        override suspend fun setupChallenge(
            challengeId: String,
            anchors: List<LocationPin>,
            targetPackages: List<String>,
        ): ChallengeSetupResult = error("unused")

        override suspend fun getMyLocation(challengeId: String): MyLocation? = error("unused")

        override suspend fun getMyScreenApps(challengeId: String): MyScreenApps? = error("unused")

        override suspend fun updateMyScreenApps(
            challengeId: String,
            apps: List<ScreenApp>,
        ): ScreenAppsUpdate = error("unused")

        override suspend fun submitObjection(
            challengeId: String,
            type: com.ruleup.verification.domain.entity.ObjectionType,
            targetDate: String,
            content: String,
            imageUrl: String?,
        ) = error("unused")

        override suspend fun decideObjection(
            challengeId: String,
            objectionId: String,
            decision: com.ruleup.verification.domain.entity.ObjectionDecision,
            reason: String?,
        ) = error("unused")

        override suspend fun getPendingReviews(challengeId: String) = error("unused")

        override suspend fun submitManual(
            challengeId: String,
            method: ManualMethod,
            targetDate: String?,
            imageUrl: String?,
            asFallback: Boolean,
        ): ManualSubmitResult = error("unused")

        override suspend fun searchPlaces(
            query: String,
            lat: Double?,
            lng: Double?,
            radiusM: Int?,
        ): List<Place> = error("unused")

        override suspend fun reverseGeocode(
            lat: Double,
            lng: Double,
        ): Place? = error("unused")
    }

    private class FakeAnalyticsLogger : AnalyticsLogger {
        override fun log(event: AnalyticsEvent) = Unit

        override fun setUserId(id: String?) = Unit

        override fun setUserProperty(
            key: String,
            value: String,
        ) = Unit
    }
}
