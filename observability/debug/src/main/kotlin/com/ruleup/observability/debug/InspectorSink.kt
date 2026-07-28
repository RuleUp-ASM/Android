package com.ruleup.observability.debug

import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink
import javax.inject.Inject

/**
 * 온디바이스 인스펙터 출구. [InspectorLog] 링버퍼에 적재한다.
 *
 * 이 모듈은 `:app` 이 `debugImplementation` 으로만 물리므로 **릴리스 APK 에 포함되지 않는다.**
 * 그래서 Hilt 멀티바인딩(`@IntoSet Sink`)도 디버그 빌드에서만 존재하고, 릴리스에서는 추가 싱크
 * 집합이 비어 있게 된다.
 *
 * `Timber.Tree` 를 대체한다. 이전에는 Timber 를 경유해야 오버레이에 뜰 수 있었지만, 이제는
 * 파이프라인을 통과한 **모든** 이벤트가 채널·심각도·화면까지 달고 여기로 온다.
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
