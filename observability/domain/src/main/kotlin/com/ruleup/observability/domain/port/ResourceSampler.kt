package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger

/** 기기 자원 스냅샷. */
fun interface ResourceSampler {
    fun sample(trigger: ProbeTrigger): PerformancePayload.ResourceProbe?

    companion object {
        /** 자원 측정이 필요 없는 조립(테스트 등)에서 쓴다. */
        val NONE = ResourceSampler { null }
    }
}
