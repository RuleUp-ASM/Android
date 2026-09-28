package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.event.ObsPayload
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.AttrValue

/** [ObsEvent] → Amplitude 이벤트 이름·속성 매핑. */
internal object AmplitudeEventMapper {
    fun eventName(payload: ObsPayload): String =
        when (payload) {
            is DiagnosticPayload -> "diagnostic"
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

            is PerformancePayload.Tti -> {
                props["page_name"] = payload.pageName
                props["total_millis"] = payload.totalMillis
                // 구간을 속성으로 편다
                payload.spans.forEach { (name, millis) -> props[name.lowercase()] = millis }
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

    /** Amplitude 는 임의 타입을 받으므로 원래 타입 그대로 편다. */
    private fun AttrValue.unwrap(): Any =
        when (this) {
            is AttrValue.Str -> v
            is AttrValue.Int64 -> v
            is AttrValue.Real -> v
            is AttrValue.Bool -> v
        }
}
