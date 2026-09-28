package com.ruleup.observability.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Sink

/** Crashlytics 출구. */
internal class CrashlyticsSink(
    private val nonFatalFloor: Severity = Severity.ERROR,
) : Sink {
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun emit(event: ObsEvent) {
        val payload = event.payload as? DiagnosticPayload ?: return
        event.context.currentScreen?.let { crashlytics.setCustomKey("screen", it.raw) }
        crashlytics.setCustomKey("tag", payload.tag)

        if (payload.severity atLeast nonFatalFloor) {
            crashlytics.recordException(payload.toThrowable())
        } else {
            crashlytics.log("[${payload.severity.name}/${payload.tag}] ${payload.message}")
        }
    }

    private fun DiagnosticPayload.toThrowable(): Throwable {
        val cause = cause
        val label =
            if (cause == null) {
                "$tag: $message"
            } else {
                "$tag: $message | ${cause.type}@${cause.stackHash}: ${cause.message}"
            }
        return ObservabilityNonFatal(label)
    }
}

/** non-fatal 기록용 합성 예외. */
internal class ObservabilityNonFatal(
    message: String,
) : RuntimeException(message)
