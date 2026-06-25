package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.SignalBatch

/**
 * 로컬 신호 버퍼 포트 (명세 §2.4·§7). Room 이 단일 진실원이며 sync 가 드레인한다.
 * 앱이 죽어도 보존되도록 OS 신호는 모두 이 버퍼를 거친다.
 */
interface SignalRepository {
    /**
     * 미전송분을 배치로 드레인한다. [collectedAt] 멱등 키를 부여해 묶고, 비어 있으면 null.
     * 200 받기 전엔 synced 미표시로 두어(중복 허용) BE 멱등이 방어한다(명세 §3.4).
     */
    suspend fun drainPending(collectedAt: String): SignalBatch?

    /** 전송 성공/폐기한 배치를 synced 표시한다(명세 §2.4). */
    suspend fun markSynced(collectedAt: String)

    /** TTL([ttlMillis]) 경과한 synced 분을 정리한다(명세 §2.4, 기본 7일). */
    suspend fun purgeExpired(ttlMillis: Long)
}
