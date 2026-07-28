package com.ruleup.observability.domain.test

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.event.ObsPayload
import com.ruleup.observability.domain.port.Sink

/**
 * 받은 이벤트를 모아두는 출구.
 *
 * [failWith] 를 채우면 [emit] 이 그 예외를 던진다 — 싱크 실패 격리를 검증할 때 쓴다.
 * `Sink.emit` 은 "예외를 던지지 않는다"가 계약이지만, **계약을 어겼을 때 파이프라인이 어떻게
 * 버티는지**가 바로 검증 대상이다.
 */
class RecordingSink(
    private val name: String = "recording",
) : Sink {
    private val recorded = mutableListOf<ObsEvent>()

    /** 던지게 하려면 채운다. 계약 위반 시나리오 주입용. */
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
