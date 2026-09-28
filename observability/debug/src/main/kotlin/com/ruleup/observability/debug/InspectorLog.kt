package com.ruleup.observability.debug

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 온디바이스 인스펙터용 링버퍼. */
object InspectorLog {
    /** 한 줄. */
    data class Entry(
        val seq: Long,
        val channel: Channel,
        val severity: Severity,
        val tag: String?,
        val message: String,
        val screen: String?,
    )

    private const val MAX_PER_CHANNEL = 100

    private val buffers = Channel.entries.associateWith { ArrayDeque<Entry>(MAX_PER_CHANNEL) }
    private var nextSeq = 0L
    private val _version = MutableStateFlow(0)

    /** 적재될 때마다 증가한다. */
    val version: StateFlow<Int> = _version.asStateFlow()

    @Synchronized
    fun add(
        channel: Channel,
        severity: Severity,
        tag: String?,
        message: String,
        screen: String?,
    ) {
        val buffer = buffers.getValue(channel)
        if (buffer.size == MAX_PER_CHANNEL) buffer.removeFirst()
        buffer.addLast(Entry(nextSeq++, channel, severity, tag, message, screen))
        _version.value += 1
    }

    /** [channels] 의 최근 [count] 줄을 시간순으로 합친다. */
    @Synchronized
    fun recent(
        channels: Set<Channel>,
        count: Int,
    ): List<Entry> =
        channels
            .flatMap { channel ->
                val buffer = buffers.getValue(channel)
                val from = (buffer.size - count).coerceAtLeast(0)
                List(buffer.size - from) { buffer[from + it] }
            }.sortedBy { it.seq }
            .takeLast(count)

    @Synchronized
    fun clear() {
        buffers.values.forEach { it.clear() }
        _version.value += 1
    }
}
