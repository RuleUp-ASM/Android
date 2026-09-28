package com.ruleup.observability.data.sink

import android.util.Log
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.port.Sink

/** Logcat 출구. */
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
