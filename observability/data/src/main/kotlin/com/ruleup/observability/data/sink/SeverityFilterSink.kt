package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Sink

/**
 * 심각도 임계값 데코레이터.
 *
 * 백엔드마다 받고 싶은 하한이 다르다(예: Crashlytics 는 WARN 이상). 이걸 어댑터 안에 숨기면
 * **[com.ruleup.observability.domain.port.Policy] 설정에도 안 잡히는 "안 찍히는 이유"** 가
 * 하나 더 생긴다. 데코레이터로 두면 배선 코드에 그대로 드러난다.
 */
internal class SeverityFilterSink(
    private val min: Severity,
    private val delegate: Sink,
) : Sink {
    override fun emit(event: ObsEvent) {
        if (event.payload.severity atLeast min) delegate.emit(event)
    }

    override fun flush() = delegate.flush()
}
