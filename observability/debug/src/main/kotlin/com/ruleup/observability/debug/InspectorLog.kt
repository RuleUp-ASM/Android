package com.ruleup.observability.debug

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 온디바이스 인스펙터용 링버퍼. **채널마다 따로 담는다** — 공유하면 말 많은 채널이 나머지를 굶는다
 * (실측에서 HTTP BODY 로깅이 다른 관측 로그를 33:1 로 압도했다).
 *
 * 흘려보내는 건 [version] 카운터뿐이고 리스트는 구독자가 [recent] 로 당겨간다.
 * `StateFlow<List<Entry>>` 로 노출하면 적재할 때마다 버퍼 전체를 복사하게 된다.
 */
object InspectorLog {
    /**
     * 한 줄. **표시에 필요한 만큼만** 담는다 — 이벤트 객체를 붙들면 버퍼가 페이로드 그래프를 살려둔다.
     * [seq] 는 채널별 버퍼를 다시 시간순으로 합칠 때 쓴다.
     */
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

    /** 적재될 때마다 증가한다. 구독자는 이 값이 바뀌면 [recent] 를 다시 읽는다. */
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

    /** [channels] 의 최근 [count] 줄을 시간순으로 합친다. **복사는 여기서만** 일어난다. */
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
