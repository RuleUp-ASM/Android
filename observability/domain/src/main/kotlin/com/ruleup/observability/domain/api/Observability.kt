package com.ruleup.observability.domain.api

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.event.ObsPayload
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ErrorInfo
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ContextProvider
import com.ruleup.observability.domain.port.Policy
import com.ruleup.observability.domain.port.Sink

/** 관측 파이프라인의 진입점. */
class Observability(
    private val clock: Clock,
    private val contextProvider: ContextProvider,
    private val profile: BuildProfile,
    @PublishedApi internal val policy: Policy,
    private val sink: Sink,
) {
    /** 진단 텍스트 이벤트. */
    inline fun log(
        severity: Severity,
        tag: String,
        cause: Throwable? = null,
        message: () -> String,
    ) {
        if (!policy.isEnabled(Channel.DIAGNOSTIC, severity, tag)) return
        logInternal(DiagnosticPayload(severity, tag, message(), cause?.let(ErrorInfo::from)))
    }

    /** 구조화 페이로드 이벤트. */
    inline fun log(
        channel: Channel,
        severity: Severity = Severity.INFO,
        tag: String? = null,
        payload: () -> ObsPayload,
    ) {
        if (!policy.isEnabled(channel, severity, tag)) return
        val built = payload()
        checkGateConsistency(channel, severity, tag, built)
        logInternal(built)
    }

    /** 출구 버퍼를 즉시 내보낸다. */
    fun flush() = sink.flush()

    /** 이벤트 채널과 페이로드 일치 검사. */
    @PublishedApi
    internal fun checkGateConsistency(
        channel: Channel,
        severity: Severity,
        tag: String?,
        payload: ObsPayload,
    ) {
        if (!profile.isDebuggable) return
        require(payload.channel == channel && payload.severity == severity && payload.tag == tag) {
            "게이트는 $channel/$severity/$tag 로 판단했는데 페이로드는 " +
                "${payload.channel}/${payload.severity}/${payload.tag} 다. " +
                "log() 의 인자와 페이로드의 channel·severity·tag 는 일치해야 한다."
        }
    }

    @PublishedApi
    internal fun logInternal(payload: ObsPayload) =
        sink.emit(
            ObsEvent(
                payload = payload,
                epochMillis = clock.epochMillis(),
                monotonicNanos = clock.monotonicNanos(),
                context = contextProvider.current(),
                profile = profile,
            ),
        )
}
