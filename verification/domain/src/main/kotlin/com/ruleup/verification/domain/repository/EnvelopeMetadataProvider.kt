package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.SignalScope

/**
 * sync envelope 의 신호 외 메타데이터 채집 포트 (전송 스펙 §0.1).
 *
 * 디바이스 시계(부팅 세션·monotonic), 권한 스냅샷, VPN, Play Integrity, 진단 heartbeat,
 * 활성 챌린지 id, 권한 부재 기반 gap 을 한 번에 모은다. Android API 의존이라 data 가 구현한다.
 */
fun interface EnvelopeMetadataProvider {
    suspend fun capture(scope: SignalScope): EnvelopeMetadata
}
