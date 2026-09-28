package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.entity.SyncPayloadTooLargeException
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.repository.EnvelopeMetadataProvider
import com.ruleup.verification.domain.repository.SignalCollector
import com.ruleup.verification.domain.repository.SignalRepository
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/** 30분 sync 유스케이스. */
class RunSyncUseCase
    @Inject
    constructor(
        private val signalCollector: SignalCollector,
        private val signalRepository: SignalRepository,
        private val envelopeMetadataProvider: EnvelopeMetadataProvider,
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            scope: SignalScope,
            collectedAt: String,
        ): SyncResult? {
            signalRepository.purgeExpired(BUFFER_TTL_MILLIS)

            // OS 신호 수집.
            signalCollector.capture(scope)

            signalRepository.purgeExpired(BUFFER_TTL_MILLIS)

            // 미전송 신호와 공백 배치 구성.
            val batch = signalRepository.drainPending(collectedAt)
            val bufferedGaps = signalRepository.drainGaps(collectedAt)
            val effectiveBatch = batch ?: SignalBatch(collectedAt = collectedAt, signals = emptyList())

            // 전송 메타데이터와 신호 공백 병합.
            val metadata = envelopeMetadataProvider.capture(scope)
            val merged = metadata.copy(gaps = metadata.gaps + bufferedGaps)

            if (metadata.activeChallengeIds.isEmpty() && effectiveBatch.isEmpty && merged.gaps.isEmpty()) return null

            // 배치 전송 및 413 분할 처리.
            val result =
                try {
                    send(merged, effectiveBatch, depth = 0)
                } catch (e: InvalidSignalPayloadException) {
                    // 400 은 봉투/계약이 틀렸다는 뜻이지 신호가 틀렸다는 뜻이 아니다.
                    throw e
                } catch (e: SyncPayloadTooLargeException) {
                    // 더 쪼갤 수 없는데도 상한을 넘는다
                    signalRepository.markSynced(collectedAt)
                    throw e
                } catch (e: SyncTooFrequentException) {
                    // 429 는 markSynced 없이 전파 → Worker 가 백오프 재시도(전송분 유지).
                    throw e
                }

            // 성공 배치 표시·구간 갱신·TTL 정리.
            signalRepository.markSynced(collectedAt)
            envelopeMetadataProvider.markCovered(merged.coverage.until)
            return result
        }

        /** 413 을 받으면 배치를 반으로 갈라 앞·뒤를 차례로 보내고 응답을 합친다. */
        private suspend fun send(
            metadata: EnvelopeMetadata,
            batch: SignalBatch,
            depth: Int,
        ): SyncResult =
            try {
                verificationRepository.sync(metadata, batch)
            } catch (e: SyncPayloadTooLargeException) {
                if (depth >= MAX_SPLIT_DEPTH) throw e
                val (head, tail) = batch.split() ?: throw e
                val headResult = send(metadata.copy(coverage = metadata.coverage.emptyAtStart()), head, depth + 1)
                val tailResult = send(metadata.copy(gaps = emptyList()), tail, depth + 1)
                headResult.mergedWith(tailResult)
            }

        companion object {
            // 전송 스펙 §0.2: 로컬 버퍼는 15일 보존 후 정리(서버 30일과의 간극은 BUFFER_EVICTED 로 표기).
            private const val BUFFER_TTL_MILLIS: Long = 15L * 24 * 60 * 60 * 1000

            // 최대 16조각까지만 쪼갠다.
            private const val MAX_SPLIT_DEPTH = 4
        }
    }
