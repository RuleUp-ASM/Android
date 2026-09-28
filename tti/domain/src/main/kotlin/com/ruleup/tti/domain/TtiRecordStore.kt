package com.ruleup.tti.domain

/** 측정 기록 저장 포트. */
interface TtiRecordStore {
    /** 기록과 그 구간을 연다. */
    suspend fun openSpan(
        tti: Tti,
        pageName: String,
        createdAt: Long,
        timeline: TtiTimeline,
        startedAt: Long,
    )

    /** 아직 안 닫힌 구간만 닫는다. */
    suspend fun closeSpan(
        tti: Tti,
        timeline: TtiTimeline,
        endedAt: Long,
    )

    /** 없으면 null. */
    suspend fun find(tti: Tti): TtiRecord?

    suspend fun findAll(): List<TtiRecord>

    suspend fun delete(ttis: List<Tti>)

    /** 끝내 완성되지 않고 남은 기록 청소. */
    suspend fun deleteCreatedBefore(threshold: Long)
}
