package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext

/** 발생한 사건 하나. */
data class ObsEvent(
    val payload: ObsPayload,
    /** 벽시계. */
    val epochMillis: Long,
    /** 단조 시계. */
    val monotonicNanos: Long,
    val profile: BuildProfile,
    val context: ObsContext,
) {
    val channel: Channel get() = payload.channel
}
