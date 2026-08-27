package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext

/**
 * 발생한 사건 하나. 조립 시점에 고정되고 이후 바뀌지 않는다.
 *
 * 집계(중복 횟수)도 이벤트 ID 도 담지 않는다 — 필요한 [com.ruleup.observability.domain.port.Sink]
 * 구현이 자기 백엔드 포맷으로 매핑할 때 붙인다.
 */
data class ObsEvent(
    val payload: ObsPayload,
    /** 벽시계. 표시·집계용이며 순서 판단에 쓰지 않는다. */
    val epochMillis: Long,
    /** 단조 시계. 이벤트 순서와 경과 시간 계산용. */
    val monotonicNanos: Long,
    val profile: BuildProfile,
    val context: ObsContext,
) {
    val channel: Channel get() = payload.channel
}
