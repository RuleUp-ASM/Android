package com.ruleup.tti.domain

import java.util.UUID

/** 측정 한 건을 가리키는 손잡이. */
data class Tti(
    val id: String = UUID.randomUUID().toString(),
)

/** 구간 하나. */
data class TtiSpan(
    val startedAt: Long,
    val endedAt: Long? = null,
) {
    /** 아직 안 닫혔으면 null. */
    val durationMillis: Long?
        get() = endedAt?.let { it - startedAt }
}

/** 저장돼 있는 측정 한 건. */
data class TtiRecord(
    val tti: Tti,
    val pageName: String,
    val createdAt: Long,
    val spans: Map<TtiTimeline, TtiSpan>,
) {
    /** 네 구간의 길이 합. */
    val totalTimeMillis: Long?
        get() =
            TtiTimeline.entries.fold(0L) { sum, timeline ->
                sum + (spans[timeline]?.durationMillis ?: return null)
            }

    /** 네 구간이 모두 닫혀 쏠 수 있는 상태인가. */
    val isComplete: Boolean
        get() = totalTimeMillis != null
}
