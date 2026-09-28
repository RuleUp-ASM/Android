package com.ruleup.observability.domain.test

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.event.ObsPayload
import com.ruleup.observability.domain.port.Sink

/** 받은 이벤트를 모아두는 출구. */
class RecordingSink(
    private val name: String = "recording",
) : Sink {
    private val recorded = mutableListOf<ObsEvent>()

    /** 채우면 [emit] 이 이 예외를 던진다. */
    var failWith: Throwable? = null

    var flushCount: Int = 0
        private set

    val events: List<ObsEvent> get() = synchronized(this) { recorded.toList() }

    val payloads: List<ObsPayload> get() = events.map { it.payload }

    val single: ObsEvent get() = events.single()

    override fun emit(event: ObsEvent) {
        failWith?.let { throw it }
        synchronized(this) { recorded += event }
    }

    override fun flush() {
        flushCount++
    }

    fun clear() {
        synchronized(this) { recorded.clear() }
    }

    override fun toString(): String = "RecordingSink($name)"
}
