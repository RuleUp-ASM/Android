package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger

/**
 * 기기 자원 스냅샷. "3초 걸렸다" 는 그때 힙이 한계였는지 저메모리였는지가 있어야 원인이 잡힌다.
 *
 * **쓰로틀 책임은 구현에 있다** — 자원 조회는 바인더 호출을 포함할 수 있어 무제한으로 부르면
 * 관측이 관측 대상을 바꾼다. 쓰로틀에 걸리면 null 이고 그건 정상 응답이다. 던지지 않는다.
 */
fun interface ResourceSampler {
    fun sample(trigger: ProbeTrigger): PerformancePayload.ResourceProbe?

    companion object {
        /** 자원 측정이 필요 없는 조립(테스트 등)에서 쓴다. */
        val NONE = ResourceSampler { null }
    }
}
