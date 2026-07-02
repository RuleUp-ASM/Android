package com.ruleup.android_ruleup.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 디버그 로그 오버레이용 인메모리 링버퍼(디버그 빌드 전용). [DebugLogTree] 가 (백그라운드 스레드 포함)
 * 적재하고 Compose 오버레이가 [logs] 를 구독한다. 최근 [MAX_LINES] 줄만 유지한다.
 */
object DebugLogStore {
    data class Entry(
        val priority: Int,
        val tag: String?,
        val message: String,
    )

    private const val MAX_LINES = 200

    private val _logs = MutableStateFlow<List<Entry>>(emptyList())
    val logs: StateFlow<List<Entry>> = _logs.asStateFlow()

    @Synchronized
    fun add(entry: Entry) {
        val next = _logs.value + entry
        _logs.value = if (next.size > MAX_LINES) next.takeLast(MAX_LINES) else next
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
