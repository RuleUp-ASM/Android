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

/**
 * 관측 파이프라인의 진입점. [Policy.isEnabled] 게이트가 **페이로드 생성 전**에 돌아, 버려질 이벤트는
 * 할당 자체가 일어나지 않는다. 완성된 페이로드를 넣는 경로를 두지 않는 이유이기도 하다.
 *
 * 예외를 잡지 않는다 — 모든 포트가 "던지지 않는다"를 계약으로 갖고, 팬아웃·라우팅·실패 격리는
 * [Sink] 구현 안에 있다. 파이프라인 설계 근거는 #159.
 */
class Observability(
    private val clock: Clock,
    private val contextProvider: ContextProvider,
    private val profile: BuildProfile,
    @PublishedApi internal val policy: Policy,
    private val sink: Sink,
) {
    /**
     * 진단 텍스트 이벤트. [message] 와 [cause] 변환은 게이트를 통과한 뒤에만 일어난다 —
     * floor 에 걸리는 로그는 문자열 연결조차 하지 않는다.
     */
    inline fun log(
        severity: Severity,
        tag: String,
        cause: Throwable? = null,
        message: () -> String,
    ) {
        if (!policy.isEnabled(Channel.DIAGNOSTIC, severity, tag)) return
        logInternal(DiagnosticPayload(severity, tag, message(), cause?.let(ErrorInfo::from)))
    }

    /**
     * 구조화 페이로드 이벤트. 게이트 인자는 [payload] 가 만들어지기 전에 필요해 따로 받으므로,
     * **페이로드의 channel·severity·tag 와 일치해야 한다** — 어긋나면 DEV/QA 에서 즉시 실패한다.
     */
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

    /** 출구 버퍼를 즉시 내보낸다. 프로세스 종료 직전에 호출한다. */
    fun flush() = sink.flush()

    /**
     * 어긋난 채로 두면 싱크 라우팅까지 엇나가 *"floor 를 올렸는데 이 로그는 왜 계속 찍히지"* 가 된다.
     * 프로덕션에서는 검사하지 않는다.
     */
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
