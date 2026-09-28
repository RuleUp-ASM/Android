package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.port.Sink
import kotlin.coroutines.cancellation.CancellationException

/** 팬아웃 + 자식별 실패 격리. */
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
                // "로깅이 실패했다"가 아니라 "JVM 이 더 못 간다"
                if (t is VirtualMachineError) throw t
                report(child, t)
                if (failure == null) failure = t
            }
        }
        if (profile.isDebuggable) failure?.let { throw it }
    }

    /** 보고 자체가 실패해도 원래 예외를 덮지 않는다 */
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
