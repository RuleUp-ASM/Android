package com.ruleup.verification.domain.usecase

import com.ruleup.analytics.AnalyticsEvent
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.port.SignalCollector
import com.ruleup.verification.domain.port.SignalRepository
import com.ruleup.verification.domain.port.VerificationRepository
import javax.inject.Inject

/**
 * 30분 sync 유스케이스 (명세 §3, Phase 2). WorkManager 가 주기적으로 호출한다.
 *
 * 흐름: 최신 OS 신호 수집 → 로컬 버퍼 드레인(멱등 키 부여) → POST sync → synced 표시.
 * - 200: synced 표시 후 결과 반환(진행률 캐시 갱신·재스케줄은 호출자 Worker).
 * - 400 INVALID_SIGNAL_PAYLOAD: 해당 배치 폐기(synced) + 예외 전파(무한 재전송 금지).
 * - 429 SYNC_TOO_FREQUENT: synced 미표시로 두고 예외 전파 → 호출자가 백오프 재시도.
 *
 * 보낼 게 없으면 null 을 반환한다(전송 생략).
 */
class RunSyncUseCase
    @Inject
    constructor(
        private val signalCollector: SignalCollector,
        private val signalRepository: SignalRepository,
        private val verificationRepository: VerificationRepository,
        private val analyticsLogger: AnalyticsLogger,
    ) {
        suspend operator fun invoke(
            scope: SignalScope,
            collectedAt: String,
        ): SyncResult? {
            // 1) UsageStats 델타 + 보조 측위를 버퍼에 적재(geofence 전이는 리시버가 이미 적재).
            signalCollector.capture(scope)

            // 2) 미전송분을 멱등 키로 묶어 드레인. 보낼 게 없으면 종료.
            val batch = signalRepository.drainPending(collectedAt) ?: return null
            if (batch.isEmpty) return null

            // 3) 전송.
            val result =
                try {
                    verificationRepository.sync(batch)
                } catch (e: InvalidSignalPayloadException) {
                    // 잘못된 배치는 폐기해 무한 재전송을 막는다.
                    signalRepository.markSynced(collectedAt)
                    analyticsLogger.log(AnalyticsEvent.VerificationSyncFailed(REASON_INVALID_PAYLOAD))
                    throw e
                } catch (e: SyncTooFrequentException) {
                    // 429 는 markSynced 없이 전파 → Worker 가 백오프 재시도(전송분 유지).
                    analyticsLogger.log(AnalyticsEvent.VerificationSyncFailed(REASON_TOO_FREQUENT))
                    throw e
                }

            // 4) 성공분 synced 표시 + TTL 정리.
            signalRepository.markSynced(collectedAt)
            signalRepository.purgeExpired(SYNCED_TTL_MILLIS)
            analyticsLogger.log(
                AnalyticsEvent.VerificationSynced(
                    updatedCount = result.updatedChallenges.size,
                    ignoredCount = result.ignoredSignalTypes.size,
                ),
            )
            return result
        }

        companion object {
            // 명세 §2.4: sync 성공분은 7일 후 정리.
            private const val SYNCED_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
            private const val REASON_INVALID_PAYLOAD = "INVALID_SIGNAL_PAYLOAD"
            private const val REASON_TOO_FREQUENT = "SYNC_TOO_FREQUENT"
        }
    }
