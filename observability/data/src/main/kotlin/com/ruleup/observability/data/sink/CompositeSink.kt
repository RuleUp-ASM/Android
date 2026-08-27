package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.port.Sink
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팬아웃 + 자식별 실패 격리. **`Sink.emit` 의 "절대 던지지 않는다" 계약을 실제로 지키는 지점**이라,
 * 여기가 *"로깅은 앱을 죽이지 않는다"* 를 지탱한다.
 *
 * 첫 실패는 모아뒀다 루프 종료 후 던져 DEV/QA 에서도 격리가 유지된다. 프로파일과 무관하게 전파하는
 * 예외와 삼키는 예외의 근거는 `CompositeSinkTest` 의 단언들에 있다.
 */
internal class CompositeSink(
    private val children: List<Sink>,
    private val profile: BuildProfile,
    private val failureReporter: SinkFailureReporter? = null,
) : Sink {
    override fun emit(event: ObsEvent) = forEachChild { it.emit(event) }

    override fun flush() = forEachChild { it.flush() }

    private inline fun forEachChild(action: (Sink) -> Unit) {
        var failure: Throwable? = null
        for (index in children.indices) {
            val child = children[index]
            try {
                action(child)
            } catch (t: Throwable) {
                // 삼키면 구조적 동시성이 조용히 깨진다.
                if (t is CancellationException) throw t
                // "로깅이 실패했다"가 아니라 "JVM 이 더 못 간다" — 삼키면 무관한 지점에서 죽는다.
                if (t is VirtualMachineError) throw t
                report(child, t)
                if (failure == null) failure = t
            }
        }
        if (profile.isDebuggable) failure?.let { throw it }
    }

    /** 보고 자체가 실패해도 원래 예외를 덮지 않는다 — 디버그 도구 버그가 진짜 원인을 가리면 안 된다. */
    private fun report(
        child: Sink,
        cause: Throwable,
    ) {
        val reporter = failureReporter ?: return
        try {
            reporter.onFailure(child::class.simpleName ?: "Sink", cause)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (t is VirtualMachineError) throw t
            if (t !== cause) cause.addSuppressed(t)
        }
    }
}
