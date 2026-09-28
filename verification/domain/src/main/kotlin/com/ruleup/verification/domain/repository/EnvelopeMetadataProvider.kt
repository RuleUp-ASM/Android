package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.SignalScope

/** sync envelope 의 신호 외 메타데이터 채집 포트. */
interface EnvelopeMetadataProvider {
    suspend fun capture(scope: SignalScope): EnvelopeMetadata

    /** 서버가 받아들인 구간 끝. */
    suspend fun markCovered(until: Long)
}
