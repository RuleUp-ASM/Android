package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger

/**
 * 기기 자원 스냅샷을 뜬다. 플랫폼 API 에 닿는 유일한 성능 측정 지점이다.
 *
 * 느렸던 순간의 상태를 붙여 두기 위한 것이다 — "이 화면이 3초 걸렸다" 는 그때 힙이 한계에
 * 붙어 있었는지, 시스템이 저메모리였는지가 있어야 원인 방향이 잡힌다.
 *
 * 계약:
 * - **쓰로틀 책임은 구현에 있다.** 호출부(jank 창·느린 TTI)는 이상이 감지될 때마다 부르고,
 *   너무 잦으면 구현이 null 을 돌려준다. 자원 조회는 바인더 호출을 포함할 수 있어
 *   무제한으로 부르면 **관측이 관측 대상을 바꾼다.**
 * - null 은 정상 응답이다. 호출부는 그냥 건너뛴다.
 * - 예외를 던지지 않는다. 임의 스레드에서 호출될 수 있다.
 */
fun interface ResourceSampler {
    fun sample(trigger: ProbeTrigger): PerformancePayload.ResourceProbe?

    companion object {
        /** 자원 측정이 필요 없는 조립(테스트 등)에서 쓴다. */
        val NONE = ResourceSampler { null }
    }
}
