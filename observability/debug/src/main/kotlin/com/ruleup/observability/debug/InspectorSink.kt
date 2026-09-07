package com.ruleup.observability.debug

import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink
import javax.inject.Inject

/**
 * 온디바이스 인스펙터 출구. [InspectorLog] 링버퍼에 적재한다. 이 모듈은 `:app` 이
 * `debugImplementation` 으로만 물어 **릴리스 APK 에 포함되지 않는다.**
 */
class InspectorSink
    @Inject
    constructor() : Sink {
        override fun emit(event: ObsEvent) {
            val payload = event.payload
            val message =
                when (payload) {
                    is DiagnosticPayload ->
                        buildString {
                            append(payload.message)
                            payload.cause?.let { append(" | ${it.type}@${it.stackHash}") }
                        }
                    else -> payload.toString()
                }
            InspectorLog.add(
                channel = event.channel,
                severity = payload.severity,
                tag = payload.tag,
                message = message,
                screen = event.context.currentScreen?.raw,
            )
        }
    }
