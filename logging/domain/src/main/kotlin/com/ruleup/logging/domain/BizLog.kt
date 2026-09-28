package com.ruleup.logging.domain

/** 기록된 이벤트 한 건. */
data class BizLog(
    val event: BizEvent,
    val screen: String?,
    val userId: String?,
    val recordedAt: Long,
)
