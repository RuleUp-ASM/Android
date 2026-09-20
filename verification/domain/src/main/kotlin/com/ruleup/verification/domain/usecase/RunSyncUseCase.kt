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

/**
 * 30분 sync 유스케이스 (전송 스펙 §0.2·§0.3). WorkManager 가 주기적으로 호출한다.
 *
 * 흐름: 최신 OS 신호 수집 → 로컬 버퍼(신호+gap) 드레인 → envelope 메타데이터 채집 → POST sync → synced 표시.
 * - 200: synced 표시 후 결과 반환(진행률 캐시 갱신·재스케줄은 호출자 Worker).
 * - 400 INVALID_SIGNAL_PAYLOAD: **신호는 남기고** 예외만 전파한다. 봉투/계약이 틀렸다는 뜻이라
 *   앱을 고치면 같은 신호가 그대로 쓸모 있다 — 폐기하면 그 구간 판정이 조용히 빠진다.
 * - 413 SYNC_PAYLOAD_TOO_LARGE: 배치를 반으로 갈라 순차 재전송하고 응답을 합친다.
 *   더 못 쪼개는데도 초과면 폐기한다 — 다시 보내도 결과가 같다.
 * - 429 SYNC_TOO_FREQUENT: synced 미표시로 두고 예외 전파 → 호출자가 백오프 재시도.
 *
 * 활성 챌린지가 있으면 신호·gap 이 없어도 빈 envelope 를 전송한다(전송 스펙 §0.5 — 서버가 공백 사유·
 * 권한 현황으로 NO_SIGNAL 을 판정). 활성 챌린지도 보낼 것도 없으면 null 을 반환한다(전송 생략).
 *
 * 구간(`coveredFrom`·`coveredUntil`)은 **전송이 받아들여진 뒤에만** 다음 시작으로 넘긴다. 실패한 전송의
 * 구간을 넘기면 그 사이 신호가 다시 선언되지 않아 서버가 "신호 없음"으로 확정한다.
 *
 * **동시에 두 번 돌리면 안 된다.** 드레인이 tagPending(전체 미전송분에 배치키 부여) → byBatch →
 * markSynced 순서라, 겹치면 뒤 실행이 앞 실행의 배치를 자기 키로 덮어써 같은 신호가 두 번 나간다.
 * 실행 경로가 Worker 하나뿐이라 직렬화는 거기서 건다(SyncGate, #355).
 */
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
            // 1) UsageStats 델타 + 보조 측위 + 움직임/수면을 버퍼에 적재(geofence 전이는 리시버가 이미 적재).
            //    수집 중 발생한 공백(HC quota·usage purge 등)은 수집기가 gap 버퍼에 적재한다.
            signalCollector.capture(scope)

            // 2) 미전송 신호·gap 을 같은 멱등 키로 묶어 드레인.
            val batch = signalRepository.drainPending(collectedAt)
            val bufferedGaps = signalRepository.drainGaps(collectedAt)
            val effectiveBatch = batch ?: SignalBatch(collectedAt = collectedAt, signals = emptyList())

            // 3) envelope 메타데이터(시계·권한·VPN·integrity·진단 + 권한 부재 기반 gap) 채집 후 버퍼형 gap 합치기.
            val metadata = envelopeMetadataProvider.capture(scope)
            val merged = metadata.copy(gaps = metadata.gaps + bufferedGaps)

            if (metadata.activeChallengeIds.isEmpty() && effectiveBatch.isEmpty && merged.gaps.isEmpty()) return null

            // 4) 전송(413 이면 반으로 갈라 재전송).
            val result =
                try {
                    send(merged, effectiveBatch, depth = 0)
                } catch (e: InvalidSignalPayloadException) {
                    // 400 은 봉투/계약이 틀렸다는 뜻이지 신호가 틀렸다는 뜻이 아니다. 여기서 synced 로
                    // 찍으면 앱 버그 한 번에 그 구간 신호가 영구히 사라지고 판정에서 조용히 빠진다.
                    // 표시하지 않으면 다음 배치가 tagPending 으로 같은 행을 다시 끌어온다.
                    throw e
                } catch (e: SyncPayloadTooLargeException) {
                    // 더 쪼갤 수 없는데도 상한을 넘는다 — 다음 주기에 다시 보내도 결과가 같다.
                    signalRepository.markSynced(collectedAt)
                    throw e
                } catch (e: SyncTooFrequentException) {
                    // 429 는 markSynced 없이 전파 → Worker 가 백오프 재시도(전송분 유지).
                    throw e
                }

            // 5) 성공분 synced 표시 + 구간 넘기기 + TTL 정리.
            signalRepository.markSynced(collectedAt)
            envelopeMetadataProvider.markCovered(merged.coverage.until)
            signalRepository.purgeExpired(BUFFER_TTL_MILLIS)
            return result
        }

        /**
         * 413 을 받으면 배치를 반으로 갈라 앞·뒤를 차례로 보내고 응답을 합친다.
         *
         * `gaps` 는 **첫 조각에만** 싣는다. 같은 공백 구간을 조각 수만큼 되풀이해 보고할 이유가 없고,
         * gap 은 판정 입력이 아니라 안내 대상 선별용이라 한 번 닿으면 충분하다.
         *
         * 구간은 반대로 **마지막 조각만** 선언한다. 앞 조각이 전체 구간을 선언한 뒤 뒤 조각이 실패하면,
         * 서버는 신호 절반만 보고 그 날을 확정해 버린다.
         *
         * 더 못 쪼개거나 [MAX_SPLIT_DEPTH] 를 넘으면 예외를 그대로 올린다 — 분할이 수렴하지 않는
         * 상황에서 요청 수만 지수로 늘리는 것을 막는다.
         */
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

            // 최대 16조각까지만 쪼갠다. 명세에 상한이 없어 정한 값이며, 그 이상은 분할로 풀리는
            // 문제가 아니라 신호 하나가 비정상적으로 큰 경우다.
            private const val MAX_SPLIT_DEPTH = 4
        }
    }
