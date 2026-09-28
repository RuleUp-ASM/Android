package com.ruleup.observability.domain.port

import com.ruleup.observability.domain.event.ObsEvent

/** 이벤트 출구. */
interface Sink {
    fun emit(event: ObsEvent)

    /** 버퍼에 쌓인 이벤트를 즉시 내보낸다. */
    fun flush() {}
}
