package com.ruleup.observability.data.sink

import android.util.Log
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.port.Sink

/**
 * Logcat 출구. **프로덕션에는 배선하지 않는다** — 배선은 `ObservabilityModule` 이 담당한다.
 *
 * 화면 오버레이는 `:observability:debug` 의 인스펙터가 별도 싱크로 맡는다. 그래서 여기서
 * Timber 를 경유할 이유가 없어졌다 — `android.util.Log` 를 직접 부른다.
 */
internal class LogcatSink : Sink {
    override fun emit(event: ObsEvent) {
        val payload = event.payload
        val tag = payload.tag ?: event.channel.name
        val screen =
            event.context.currentScreen
                ?.raw
                ?.let { " @$it" } ?: ""
        val message =
            when (payload) {
                is DiagnosticPayload ->
                    buildString {
                        append(payload.message)
                        payload.cause?.let { append(" | ${it.type}@${it.stackHash}: ${it.message}") }
                    }
                else -> payload.toString()
            }
        Log.println(payload.severity.toLogPriority(), tag, "$message$screen")
    }

    private fun Severity.toLogPriority(): Int =
        when (this) {
            Severity.VERBOSE -> Log.VERBOSE
            Severity.DEBUG -> Log.DEBUG
            Severity.INFO -> Log.INFO
            Severity.WARN -> Log.WARN
            Severity.ERROR -> Log.ERROR
        }
}
