package com.ruleup.logging.domain.test

import com.ruleup.logging.domain.BizEvent
import com.ruleup.logging.domain.BizLogger

/** 받은 이벤트를 그 자리에서 모아두는 기록기. */
class RecordingBizLogger : BizLogger {
    private val recorded = mutableListOf<BizEvent>()

    var initCount: Int = 0
        private set

    var destroyCount: Int = 0
        private set

    val events: List<BizEvent> get() = synchronized(this) { recorded.toList() }

    /** 기록된 이벤트 이름만. */
    val names: List<String> get() = events.map { it.name }

    fun eventsNamed(name: String): List<BizEvent> = events.filter { it.name == name }

    override fun init() {
        initCount++
    }

    override fun record(event: BizEvent) {
        synchronized(this) { recorded += event }
    }

    override fun destroy() {
        destroyCount++
    }

    fun clear() {
        synchronized(this) { recorded.clear() }
    }
}
