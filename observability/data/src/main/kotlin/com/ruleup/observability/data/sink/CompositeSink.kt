package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.port.Sink
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팬아웃 + 자식별 실패 격리. 도메인이 아는 유일한 출구가 이것이다.
 *
 * **`Sink.emit` 의 "절대 던지지 않는다" 계약을 실제로 지키는 지점**이다. 도메인은 방어 코드를
 * 두지 않으므로, 여기가 *"로깅은 앱을 죽이지 않는다"* 를 지탱한다.
 *
 * 첫 실패를 모아뒀다가 루프 종료 후 던지므로, DEV/QA 에서도 **격리가 유지된 채** 시끄럽다 —
 * 3번 자식이 죽어도 4번은 실행되고, 개발자는 여전히 예외를 본다.
 *
 * 두 가지는 프로파일과 무관하게 전파한다:
 * - `CancellationException` — 삼키면 구조적 동시성이 조용히 깨진다.
 * - `VirtualMachineError`(OOM·StackOverflow) — "로깅이 실패했다"가 아니라 "JVM 이 더 못 간다"다.
 *   삼키면 무관한 지점에서 죽어 원인 추적이 불가능해진다.
 *   반면 `NoClassDefFoundError` 같은 `LinkageError` 는 삼킨다 — 그건 SDK 누락·R8 설정 같은
 *   패키징 버그라 사용자 앱을 죽일 이유가 없다.
 *
 * 프로덕션에서 삼킨 실패는 [SinkFailureReporter] 가 남긴다. **여기서 한 번은 기록되어야**
 * "수집이 멈춘 상태"와 "이벤트가 없는 상태"가 구분된다.
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
                if (t is CancellationException) throw t
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
