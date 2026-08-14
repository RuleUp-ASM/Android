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
 * 관측 파이프라인의 유일한 진입점.
 *
 * 흐름: **게이트 → 조립 → [Sink]**
 *
 * 진입점은 둘뿐이다:
 * - [log] `(severity, tag, cause) { message }` — 진단 텍스트
 * - [log] `(channel, severity, tag) { payload }` — 구조화 페이로드(전 채널)
 *
 * 둘 다 [Policy.isEnabled] 게이트가 **페이로드 생성 전**에 돌기 때문에, 버려질 이벤트는
 * 객체·문자열 할당 자체가 발생하지 않는다. 이미 만들어진 페이로드를 그대로 넣는 경로는
 * 두지 않는다 — 게이트가 할당 뒤에 돌아 이 보호가 무력해지고, 실제로 그럴 필요가 있는
 * 호출부도 없다.
 *
 * `v`/`d`/`i`/`w`/`e` 단축 호출은 `Shorthands.kt` 의 확장 함수로 제공된다.
 *
 * **예외를 잡지 않는다.** 모든 포트가 "던지지 않는다"를 계약으로 갖고, 그 계약을 지키는 것은
 * 어댑터의 책임이다. 특히 백엔드 팬아웃·채널 라우팅·실패 격리는 [Sink] 구현 안에 있다 —
 * [Sink.emit] 이 논블로킹이라 **진짜 실패는 비동기로 일어나 여기까지 오지도 않기 때문**이다.
 * 도메인이 방어 코드를 두면 잡지도 못하는 것을 잡는 시늉만 하면서 인프라 정책만 끌어안게 된다.
 *
 * 도메인이 아는 판단은 [Policy.isEnabled] 게이트뿐이고, 그건 **페이로드 생성 전에 호출되어야 해서**
 * 여기 있을 수밖에 없다. 전송 정책(재시도·레이트리밋 등)은 전부 [Sink] 구현 안에 있다.
 */
class Observability(
    private val clock: Clock,
    private val contextProvider: ContextProvider,
    private val profile: BuildProfile,
    @PublishedApi internal val policy: Policy,
    private val sink: Sink,
) {
    /**
     * 진단 텍스트 이벤트. 가장 흔한 경로다.
     *
     * [message] 는 게이트를 통과한 뒤에만 평가되므로, floor 에 걸리는 로그는 문자열 연결조차
     * 일어나지 않는다. [cause] 는 통과 후 [ErrorInfo] 로 변환된다.
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
     * 구조화 페이로드 이벤트. 진단·비즈니스·성능 전 채널을 덮는다.
     *
     * 스크롤 중 `UserAction`, 프레임 콜백발 `JankWindow` 처럼 폭주하기 쉬운 경로일수록
     * 게이트가 [payload] 생성 전에 도는 이 형태가 값을 한다.
     *
     * 게이트 인자는 페이로드가 만들어지기 전에 필요하므로 따로 받는다. 그래서 [payload] 가
     * 만드는 값과 어긋날 수 있는데, DEV/QA 에서 [checkGateConsistency] 가 즉시 잡는다.
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
     * 게이트 인자와 실제 페이로드가 어긋나면 DEV/QA 에서 즉시 실패시킨다.
     *
     * 어긋나면 게이트는 A 기준으로 통과시키고 기록은 B 로 남는다. 채널이 어긋나면 싱크 라우팅까지
     * 엇나가서 *"floor 를 올렸는데 이 로그는 왜 계속 찍히지"* 로 나타난다. 프로덕션에서는 검사하지 않는다.
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
