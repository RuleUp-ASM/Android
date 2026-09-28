package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Sink

/** 심각도 임계값 데코레이터. */
internal class SeverityFilterSink(
    private val min: Severity,
    private val delegate: Sink,
) : Sink {
    override fun emit(event: ObsEvent) {
        if (event.payload.severity atLeast min) delegate.emit(event)
    }

    override fun flush() = delegate.flush()
}
