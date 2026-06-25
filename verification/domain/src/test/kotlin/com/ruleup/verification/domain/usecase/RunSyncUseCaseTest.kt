package com.ruleup.verification.domain.usecase

import com.ruleup.analytics.AnalyticsEvent
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.LocationPoint
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.entity.VerificationSignal
import com.ruleup.verification.domain.port.SignalCollector
import com.ruleup.verification.domain.port.SignalRepository
import com.ruleup.verification.domain.port.VerificationRepository
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
    fun `보낼 게 없으면 전송하지 않고 null 을 반환한다`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = null)
            val verificationRepo = FakeVerificationRepository()
            val useCase = RunSyncUseCase(FakeSignalCollector(), signalRepo, verificationRepo, FakeAnalyticsLogger())

            val result = useCase(scope, collectedAt)

            assertNull(result)
            assertFalse(verificationRepo.syncCalled)
            assertFalse(signalRepo.markSyncedCalled)
        }

    @Test
    fun `성공하면 markSynced 후 결과를 반환한다 - 누적분 전송`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = nonEmptyBatch())
            val verificationRepo = FakeVerificationRepository(result = syncResult())
            val useCase = RunSyncUseCase(FakeSignalCollector(), signalRepo, verificationRepo, FakeAnalyticsLogger())

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
            val useCase = RunSyncUseCase(FakeSignalCollector(), signalRepo, verificationRepo, FakeAnalyticsLogger())

            assertFailsWith<InvalidSignalPayloadException> { useCase(scope, collectedAt) }
            // 폐기 = synced 표시(무한 재전송 금지).
            assertTrue(signalRepo.markSyncedCalled)
        }

    @Test
    fun `429 SYNC_TOO_FREQUENT 은 markSynced 없이 예외 전파한다 - 백오프 재시도용`() =
        runBlocking {
            val signalRepo = FakeSignalRepository(drain = nonEmptyBatch())
            val verificationRepo = FakeVerificationRepository(error = SyncTooFrequentException())
            val useCase = RunSyncUseCase(FakeSignalCollector(), signalRepo, verificationRepo, FakeAnalyticsLogger())

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
    ) : SignalRepository {
        var markSyncedCalled = false
        var purgeCalled = false

        override suspend fun drainPending(collectedAt: String): SignalBatch? = drain

        override suspend fun markSynced(collectedAt: String) {
            markSyncedCalled = true
        }

        override suspend fun purgeExpired(ttlMillis: Long) {
            purgeCalled = true
        }
    }

    private class FakeVerificationRepository(
        private val result: SyncResult? = null,
        private val error: Throwable? = null,
    ) : VerificationRepository {
        var syncCalled = false

        override suspend fun sync(batch: SignalBatch): SyncResult {
            syncCalled = true
            error?.let { throw it }
            return requireNotNull(result)
        }

        override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot = error("unused")

        override suspend fun getVerificationDetail(
            challengeId: String,
            logDays: Int,
        ): VerificationDetail = error("unused")

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
