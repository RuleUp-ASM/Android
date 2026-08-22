package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap

/**
 * 로컬 신호 버퍼 포트 (명세 §2.4·§7). Room 이 단일 진실원이며 sync 가 드레인한다.
 * 앱이 죽어도 보존되도록 OS 신호는 모두 이 버퍼를 거친다.
 */
interface SignalRepository {
    /**
     * 미전송분을 배치로 드레인한다. [collectedAt] 멱등 키를 부여해 묶고, 비어 있으면 null.
     * 200 받기 전엔 synced 미표시로 두어(중복 허용) BE 멱등이 방어한다(명세 §3.4).
     *
     * **앞 배치가 실패해 남은 신호도 이번 배치에 합쳐 끌어온다**(전송 스펙 「오프라인 복구」).
     * 실패분을 남겨만 두고 다시 싣지 않으면 그 신호는 버퍼에 갇힌 채 판정에서 조용히 빠진다.
     */
    suspend fun drainPending(collectedAt: String): SignalBatch?

    /**
     * 버퍼형 신호 공백(수집기가 적재한 [SignalGap])을 같은 배치 키로 드레인한다(전송 스펙 §0.5).
     * 신호 배치와 함께 envelope `gaps[]` 로 전송된다. 권한 부재 기반 계산형 gap 은 별도(메타데이터 provider).
     */
    suspend fun drainGaps(collectedAt: String): List<SignalGap>

    /** 전송 성공/폐기한 배치(신호+gap)를 synced 표시한다(명세 §2.4). */
    suspend fun markSynced(collectedAt: String)

    /** TTL([ttlMillis]) 경과한 synced 분을 정리한다(명세 §2.4, 버퍼 보존 15일). */
    suspend fun purgeExpired(ttlMillis: Long)
}
