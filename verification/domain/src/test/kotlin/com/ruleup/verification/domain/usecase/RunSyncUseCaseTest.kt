package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceClock
import com.ruleup.verification.domain.entity.DeviceDiagnostics
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthReading
import com.ruleup.verification.domain.entity.IntegritySnapshot
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.LocationPoint
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.NetworkState
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.RecordingMethod
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.entity.SyncPayloadTooLargeException
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
import kotlin.test.assertNotNull
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
                )

            assertFailsWith<SyncTooFrequentException> { useCase(scope, collectedAt) }
            // 전송분 유지(다음 백오프 재시도가 다시 보냄).
            assertFalse(signalRepo.markSyncedCalled)
        }

    @Test
    fun `413 이면 배치를 반으로 갈라 나눠 보낸다`() =
        runBlocking {
            // 다음 주기로 미루면 같은 크기로 다시 막힌다 — 이번 실행 안에서 쪼개야 신호가 나간다.
            val signalRepo = FakeSignalRepository(drain = healthBatch(readings = 4))
            val verificationRepo =
                FakeVerificationRepository(
                    maxEvents = 2,
                    resultFor = { syncResult(challengeId = "c" + it.eventCount()) },
                )
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(activeChallengeIds = listOf("c1")),
                    verificationRepo,
                )

            val result = useCase(scope, collectedAt)

            // 원본 1회(413) + 조각 2회.
            assertEquals(3, verificationRepo.attempts)
            assertEquals(listOf(2, 2), verificationRepo.acceptedBatches.map { it.eventCount() })
            assertNotNull(result)
            assertTrue(signalRepo.markSyncedCalled)
        }

    @Test
    fun `쪼갠 조각들의 갱신이 하나로 합쳐진다`() =
        runBlocking {
            // 앞 조각의 updatedChallenges 를 버리면 그 챌린지의 진행률 캐시가 이번 sync 를 통째로 놓친다.
            var seq = 0
            val verificationRepo =
                FakeVerificationRepository(
                    maxEvents = 2,
                    resultFor = { syncResult(challengeId = "c" + seq++) },
                )
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    FakeSignalRepository(drain = healthBatch(readings = 4)),
                    FakeEnvelopeMetadataProvider(activeChallengeIds = listOf("c1")),
                    verificationRepo,
                )

            val result = useCase(scope, collectedAt)

            assertEquals(listOf("c0", "c1"), result?.updatedChallenges?.map { it.challengeId })
        }

    @Test
    fun `gap 은 첫 조각에만 실린다`() =
        runBlocking {
            // 같은 공백 구간을 조각 수만큼 되풀이해 보고할 이유가 없다 — 판정 입력이 아니라 안내용이다.
            val verificationRepo = FakeVerificationRepository(maxEvents = 2, resultFor = { syncResult() })
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    FakeSignalRepository(drain = healthBatch(readings = 4), gaps = listOf(gap())),
                    FakeEnvelopeMetadataProvider(activeChallengeIds = listOf("c1")),
                    verificationRepo,
                )

            useCase(scope, collectedAt)

            assertEquals(listOf(1, 0), verificationRepo.acceptedGapCounts)
        }

    @Test
    fun `더 쪼갤 수 없는데도 413 이면 폐기한다`() =
        runBlocking {
            // 다음 주기에 같은 배치를 다시 보내도 결과가 같다 — 버퍼에 남겨 두면 영영 막힌다.
            val signalRepo = FakeSignalRepository(drain = healthBatch(readings = 1))
            val useCase =
                RunSyncUseCase(
                    FakeSignalCollector(),
                    signalRepo,
                    FakeEnvelopeMetadataProvider(activeChallengeIds = listOf("c1")),
                    FakeVerificationRepository(maxEvents = 0, resultFor = { syncResult() }),
                )

            assertFailsWith<SyncPayloadTooLargeException> { useCase(scope, collectedAt) }
            assertTrue(signalRepo.markSyncedCalled)
        }

    private fun healthBatch(readings: Int): SignalBatch =
        SignalBatch(
            collectedAt = collectedAt,
            signals =
                listOf(
                    VerificationSignal.Health(
                        date = "2026-06-24",
                        metric = HealthMetric.STEPS,
                        readings =
                            (1..readings).map {
                                HealthReading(
                                    recordId = "hc-" + it,
                                    value = it.toDouble(),
                                    startTime = it.toLong(),
                                    endTime = it.toLong(),
                                    recordingMethod = RecordingMethod.AUTO,
                                    originPackage = "com.sec.android.app.shealth",
                                )
                            },
                    ),
                ),
        )

    private fun gap(): SignalGap =
        SignalGap(
            signalType = "HEALTH",
            reason = GapReason.PERMISSION_MISSING,
            fromMillis = 0L,
            toMillis = 1L,
            recoverable = true,
        )

    private fun nonEmptyBatch(): SignalBatch =
        SignalBatch(
            collectedAt = collectedAt,
            signals = listOf(VerificationSignal.Locations(listOf(LocationPoint(37.0, 127.0, 5f, false, 1L)))),
        )

    private fun syncResult(challengeId: String = "c1"): SyncResult =
        SyncResult(
            syncedAt = collectedAt,
            flushIntervalSec = 1800,
            maxPayloadBytes = 1_048_576,
            updatedChallenges =
                listOf(
                    com.ruleup.verification.domain.entity.UpdatedChallenge(
                        challengeId,
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
        // 서버 상한 흉내 — 이벤트 수가 이 값을 넘으면 413 을 던진다. null 이면 상한 없음.
        private val maxEvents: Int? = null,
        private val resultFor: ((SignalBatch) -> SyncResult)? = null,
    ) : VerificationRepository {
        var syncCalled = false
        var syncedBatch: SignalBatch? = null
        var attempts = 0
        val acceptedBatches = mutableListOf<SignalBatch>()
        val acceptedGapCounts = mutableListOf<Int>()

        override suspend fun submitIntro(intro: DeviceIntro): SyncPolicy = error("unused")

        override suspend fun sync(
            metadata: EnvelopeMetadata,
            batch: SignalBatch,
        ): SyncResult {
            syncCalled = true
            syncedBatch = batch
            attempts++
            error?.let { throw it }
            if (maxEvents != null && batch.eventCount() > maxEvents) throw SyncPayloadTooLargeException()
            acceptedBatches += batch
            acceptedGapCounts += metadata.gaps.size
            return resultFor?.invoke(batch) ?: requireNotNull(result)
        }

        override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot = error("unused")

        override suspend fun getTodayResult(challengeId: String) = error("unused")

        override suspend fun getVerificationDetail(
            challengeId: String,
            logDays: Int,
        ): VerificationDetail = error("unused")

        override suspend fun setupChallenge(
            challengeId: String,
            anchors: AnchorSet,
            targetPackages: List<String>,
        ): ChallengeSetupResult = error("unused")

        override suspend fun getMyLocation(challengeId: String): MyLocation? = error("unused")

        override suspend fun updateMyLocation(
            challengeId: String,
            anchors: AnchorSet,
        ): MyLocation = error("unused")

        override suspend fun acknowledgeResult(verificationId: String) = error("unused")

        override suspend fun cancelManual(verificationId: String) = error("unused")

        override suspend fun getMyScreenApps(challengeId: String): MyScreenApps? = error("unused")

        override suspend fun updateMyScreenApps(
            challengeId: String,
            apps: ScreenAppSet,
        ): ScreenAppsUpdate = error("unused")

        override suspend fun submitAppeal(
            verificationId: String,
            reason: String,
            imageUrl: String?,
        ) = error("unused")

        override suspend fun submitManual(
            challengeId: String,
            targetDate: String?,
            note: String?,
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
}

/** 배치가 실어 보내는 이벤트 총 개수 — 테스트에서 서버 상한을 흉내 낼 때 쓴다. */
private fun SignalBatch.eventCount(): Int =
    signals.sumOf { signal ->
        when (signal) {
            is VerificationSignal.GeofenceTransitions -> signal.events.size
            is VerificationSignal.ScreenTime -> signal.appEvents.size
            is VerificationSignal.Locations -> signal.points.size
            is VerificationSignal.Health -> signal.readings.size
            is VerificationSignal.Sleep -> signal.sessions.size
            is VerificationSignal.Wake -> 1
        }
    }
