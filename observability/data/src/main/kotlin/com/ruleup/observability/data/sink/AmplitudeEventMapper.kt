package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.event.ObsPayload
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.AttrValue

/**
 * [ObsEvent] → Amplitude 이벤트 이름·속성 매핑. 이름은 [FirebaseEventMapper] 와 **같은 값을 쓴다** —
 * 갈리면 같은 지표를 두 번 정의하게 된다.
 *
 * **절단 로직이 없다.** Firebase 의 40자·25개 제한이 Amplitude 엔 없으므로, 두 도구를 대조할 때
 * 값이 다르면 Firebase 쪽이 잘린 것이다.
 */
internal object AmplitudeEventMapper {
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
        }

    fun toProperties(event: ObsEvent): MutableMap<String, Any?> {
        val props = mutableMapOf<String, Any?>()
        event.context.currentScreen?.let { props["screen"] = it.raw }
        putPayloadFields(props, event.payload)
        event.payload.attrs.entries
            .forEach { (key, value) -> props[key.raw] = value.unwrap() }
        return props
    }

    private fun putPayloadFields(
        props: MutableMap<String, Any?>,
        payload: ObsPayload,
    ) {
        when (payload) {
            is DiagnosticPayload -> {
                props["severity"] = payload.severity.name
                props["tag"] = payload.tag
                props["message"] = payload.message
                payload.cause?.let {
                    props["error_type"] = it.type
                    props["error_hash"] = it.stackHash
                }
            }

            is BusinessPayload.ScreenView -> {
                props["screen_name"] = payload.screen.raw
                payload.referrer?.let { props["from_screen"] = it.fromScreen.raw }
            }

            is BusinessPayload.UserAction -> {
                props["screen_name"] = payload.screen.raw
                props["element"] = payload.element.raw
            }

            // Custom 은 이름과 attrs 가 전부다. 이름은 eventName() 이, attrs 는 공통 루프가 담는다.
            is BusinessPayload.Custom -> Unit

            is PerformancePayload.Tti -> {
                props["screen_name"] = payload.screen.raw
                props["total_millis"] = payload.totalMillis
                props["outcome"] = payload.outcome.name
            }

            is PerformancePayload.JankWindow -> {
                props["screen_name"] = payload.screen.raw
                props["total_frames"] = payload.totalFrames
                props["janky_frames"] = payload.jankyFrames
                props["frozen_frames"] = payload.frozenFrames
                props["p95_frame_millis"] = payload.p95FrameMillis
            }

            is PerformancePayload.ResourceProbe -> {
                props["trigger"] = payload.trigger.name
                props["heap_used_bytes"] = payload.heapUsedBytes
                props["heap_max_bytes"] = payload.heapMaxBytes
                props["low_memory"] = payload.lowMemory
            }
        }
    }

    /**
     * Amplitude 는 임의 타입을 받으므로 원래 타입 그대로 편다. Firebase 처럼 Boolean 을 0/1 Long 으로
     * 바꾸지 않는다 — 대시보드에서 true/false 로 읽히는 편이 낫다.
     */
    private fun AttrValue.unwrap(): Any =
        when (this) {
            is AttrValue.Str -> v
            is AttrValue.Int64 -> v
            is AttrValue.Real -> v
            is AttrValue.Bool -> v
        }
}
