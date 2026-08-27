package com.ruleup.observability.data.sink

import android.os.Bundle
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.event.ObsPayload
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.AttrValue
import java.util.concurrent.atomic.AtomicLong

/**
 * [ObsEvent] → Firebase Analytics 매핑. Firebase 제약을 **여기서 흡수한다** — 이름 40자·키 40자·
 * 값 100자·이벤트당 파라미터 25개, 값 타입은 `String`·`Long`·`Double` 뿐이라 `Boolean` 은 0/1 `Long` 이다.
 *
 * **절단은 조용히 일어난다.** 잘린 값은 분석에서 다른 값과 뭉치므로 [truncated] 로 세어
 * `ObservabilityDiagnostics` 가 인스펙터에 노출한다.
 */
internal object FirebaseEventMapper {
    private const val MAX_NAME = 40
    private const val MAX_KEY = 40
    private const val MAX_VALUE = 100
    private const val MAX_PARAMS = 25

    // 임의 스레드에서 동시에 증가한다. @Volatile 은 read-modify-write 를 보호하지 못해
    // 안전해 보이면서 카운트가 유실된다.
    private val truncations = AtomicLong()

    /** 절단이 발생한 누적 횟수. 개발 중 매핑 손실을 눈치채기 위한 진단값이다. */
    val truncated: Long get() = truncations.get()

    fun eventName(payload: ObsPayload): String =
        when (payload) {
            is DiagnosticPayload -> "diagnostic"
            is BusinessPayload.ScreenView -> "screen_view"
            is BusinessPayload.UserAction -> "user_action"
            // feature 팩토리가 선언한 이름을 그대로 쓴다.
            is BusinessPayload.Custom -> payload.name
            is PerformancePayload.Tti -> "perf_tti"
            is PerformancePayload.JankWindow -> "perf_jank"
            is PerformancePayload.ResourceProbe -> "perf_resource"
        }.take(MAX_NAME)

    fun toBundle(event: ObsEvent): Bundle {
        val bundle = Bundle()
        event.context.currentScreen?.let { bundle.putString("screen", it.raw.clampValue()) }
        putPayloadFields(bundle, event.payload)

        var count = bundle.size()
        for ((key, value) in event.payload.attrs.entries) {
            if (count >= MAX_PARAMS) {
                truncations.incrementAndGet()
                break
            }
            bundle.put(key.raw.clampKey(), value)
            count++
        }
        return bundle
    }

    private fun putPayloadFields(
        bundle: Bundle,
        payload: ObsPayload,
    ) {
        when (payload) {
            is DiagnosticPayload -> {
                bundle.putString("severity", payload.severity.name)
                bundle.putString("tag", payload.tag.clampValue())
                bundle.putString("message", payload.message.clampValue())
                payload.cause?.let {
                    bundle.putString("error_type", it.type.clampValue())
                    bundle.putString("error_hash", it.stackHash)
                }
            }

            is BusinessPayload.ScreenView -> {
                bundle.putString("screen_name", payload.screen.raw.clampValue())
                payload.referrer?.let { bundle.putString("from_screen", it.fromScreen.raw.clampValue()) }
            }

            is BusinessPayload.UserAction -> {
                bundle.putString("screen_name", payload.screen.raw.clampValue())
                bundle.putString("element", payload.element.raw.clampValue())
            }

            // Custom 은 이름과 attrs 가 전부다. 이름은 eventName() 이, attrs 는 아래 공통 루프가 담는다.
            is BusinessPayload.Custom -> Unit

            is PerformancePayload.Tti -> {
                bundle.putString("screen_name", payload.screen.raw.clampValue())
                bundle.putLong("total_millis", payload.totalMillis)
                bundle.putString("outcome", payload.outcome.name)
            }

            is PerformancePayload.JankWindow -> {
                bundle.putString("screen_name", payload.screen.raw.clampValue())
                bundle.putLong("total_frames", payload.totalFrames.toLong())
                bundle.putLong("janky_frames", payload.jankyFrames.toLong())
                bundle.putLong("frozen_frames", payload.frozenFrames.toLong())
                bundle.putLong("p95_frame_millis", payload.p95FrameMillis)
            }

            is PerformancePayload.ResourceProbe -> {
                bundle.putString("trigger", payload.trigger.name)
                bundle.putLong("heap_used_bytes", payload.heapUsedBytes)
                bundle.putLong("heap_max_bytes", payload.heapMaxBytes)
                bundle.putLong("low_memory", if (payload.lowMemory) 1L else 0L)
            }
        }
    }

    private fun Bundle.put(
        key: String,
        value: AttrValue,
    ) {
        when (value) {
            is AttrValue.Str -> putString(key, value.v.clampValue())
            is AttrValue.Int64 -> putLong(key, value.v)
            is AttrValue.Real -> putDouble(key, value.v)
            is AttrValue.Bool -> putLong(key, if (value.v) 1L else 0L)
        }
    }

    private fun String.clampKey(): String = if (length <= MAX_KEY) this else take(MAX_KEY).also { truncations.incrementAndGet() }

    private fun String.clampValue(): String = if (length <= MAX_VALUE) this else take(MAX_VALUE).also { truncations.incrementAndGet() }
}
