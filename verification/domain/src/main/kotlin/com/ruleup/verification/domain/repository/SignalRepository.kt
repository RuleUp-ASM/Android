package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap

/** 로컬 신호 버퍼 포트. */
interface SignalRepository {
    /** 미전송분을 배치로 드레인한다. */
    suspend fun drainPending(collectedAt: String): SignalBatch?

    /** 버퍼형 신호 공백(수집기가 적재한 [SignalGap])을 같은 배치 키로 드레인한다. */
    suspend fun drainGaps(collectedAt: String): List<SignalGap>

    /** 전송 성공/폐기한 배치(신호+gap)를 synced 표시한다. */
    suspend fun markSynced(collectedAt: String)

    /** 보존 기간·건수 상한을 넘은 버퍼를 정리한다. */
    suspend fun purgeExpired(ttlMillis: Long)
}
