package com.ruleup.observability.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Sink

/**
 * Crashlytics 출구. 진단 채널만 받는 것을 전제로 하며, 라우팅은 [ChannelFilterSink] 가 한다.
 *
 * uncaught 크래시·ANR 은 SDK 가 자동 수집하므로, 여기서는 **비즈니스 코드가 잡아서 보고한
 * 예외**를 non-fatal 로 보강한다. 그 경로가 죽으면 에러가 발생했다는 사실 자체가 사라지므로,
 * 파이프라인에서 가장 유실되면 안 되는 출구다.
 *
 * [DiagnosticPayload.cause] 가 값 타입([com.ruleup.observability.domain.model.ErrorInfo])이라
 * 원래 `Throwable` 이 없다. non-fatal 기록에는 스택이 필요하므로 **스택 해시를 심은 합성 예외**를
 * 만들어 넘긴다 — 같은 지점의 예외가 같은 이슈로 묶이도록 메시지에 `stackHash` 를 포함한다.
 *
 * **`flush()` 를 재정의하지 않는다.** `sendUnsentReports()` 는 수집 동의(opt-in) 플로우 전용 API 로,
 * `firebase_crashlytics_collection_enabled = false` 로 보류해 둔 리포트를 동의 시점에 올리는 용도다.
 * 자동 수집을 켜둔 지금은 보류분 자체가 없고(다음 앱 실행 때 SDK 가 알아서 올린다) 크래시 도중에는
 * 업로드를 끝낼 수도 없다. 게다가 `onTrimMemory` 경로로도 불리므로, 나중에 동의 게이팅을 도입하면
 * **백그라운드 전환마다 동의 없이 업로드하는 경로**가 된다. 자체 버퍼가 없으니 기본 no-op 이 맞다.
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
 * non-fatal 기록용 합성 예외.
 *
 * 원본 `Throwable` 이 값 타입으로 변환된 뒤라 스택이 없다. 이 예외의 스택은 파이프라인 내부를
 * 가리키므로 **의미가 없고**, Crashlytics 의 이슈 그룹핑은 메시지에 담긴 `stackHash` 로 이뤄진다.
 */
internal class ObservabilityNonFatal(
    message: String,
) : RuntimeException(message)
