package com.ruleup.logging.domain.test

import com.ruleup.logging.domain.BizEvent
import com.ruleup.logging.domain.BizLogger

/**
 * 받은 이벤트를 그 자리에서 모아두는 기록기.
 *
 * 실제 구현은 전송을 IO 로 넘겨 나중에 도는데, 그러면 "이 화면이 이 이벤트를 남기는가" 를 보려는
 * 테스트가 스케줄러를 돌려야 한다. 대역은 동기라 기록 직후 바로 읽는다 — 전송 순서·실패 격리는
 * `BizLoggerImplTest` 가 실제 구현으로 본다.
 */
class RecordingBizLogger : BizLogger {
    private val recorded = mutableListOf<BizEvent>()

    var initCount: Int = 0
        private set

    var destroyCount: Int = 0
        private set

    val events: List<BizEvent> get() = synchronized(this) { recorded.toList() }

    /** 기록된 이벤트 이름만. 이름이 곧 분석 백엔드와의 계약이라 대부분의 단언이 이것으로 끝난다. */
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
