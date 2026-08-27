package com.ruleup.observability.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Sink

/**
 * Crashlytics 출구. 진단 채널만 받고 라우팅은 [ChannelFilterSink] 가 한다. uncaught 크래시는 SDK 가
 * 자동 수집하므로 여기서는 **잡아서 보고한 예외**를 non-fatal 로 보강한다.
 *
 * [DiagnosticPayload.cause] 가 값 타입이라 원본 `Throwable` 이 없다 — 스택 해시를 메시지에 심은
 * 합성 예외로 넘겨 같은 지점의 예외가 같은 이슈로 묶이게 한다.
 *
 * **`flush()` 를 재정의하지 않는다.** `sendUnsentReports()` 는 수집 동의(opt-in) 전용 API 라, 나중에
 * 동의 게이팅을 도입하면 `onTrimMemory` 마다 동의 없이 업로드하는 경로가 된다.
 */
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

/**
 * non-fatal 기록용 합성 예외. 이 예외의 스택은 파이프라인 내부를 가리켜 **의미가 없다** —
 * Crashlytics 의 이슈 그룹핑은 메시지에 담긴 `stackHash` 로 이뤄진다.
 */
internal class ObservabilityNonFatal(
    message: String,
) : RuntimeException(message)
