package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext

/**
 * 발생한 사건 하나. 조립 시점에 고정되고 이후 바뀌지 않는다.
 *
 * **한 번 일어난 일만 기술한다.** "같은 사건이 몇 번 눌렸는지" 같은 집계는 전송 정책의 산물이라
 * 여기 담지 않는다 — 중복 제거를 수행하는 [com.ruleup.observability.domain.port.Sink] 구현이
 * 자기 백엔드 포맷으로 매핑할 때 붙인다.
 *
 * 식별자도 없다. 분석 백엔드는 이벤트 ID 를 요구하지 않고, 순서는 [monotonicNanos] 로 충분하다.
 * 자체 서버 전송처럼 멱등 키가 필요해지는 싱크가 생기면 **그 싱크가 발급**한다 — 이벤트를
 * 캡처 시점에 동기적으로 받으므로 도메인이 미리 붙여둘 이유가 없다.
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
