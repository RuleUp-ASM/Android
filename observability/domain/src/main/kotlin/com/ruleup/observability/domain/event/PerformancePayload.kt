package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.ProbeTrigger
import com.ruleup.observability.domain.model.ScreenKey

sealed interface PerformancePayload : ObsPayload {
    override val channel: Channel get() = Channel.PERFORMANCE

    /** 화면이 쓸 수 있게 되기까지의 소요. */
    data class Tti(
        val pageName: String,
        val totalMillis: Long,
        val spans: Map<String, Long>,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : PerformancePayload

    data class JankWindow(
        val screen: ScreenKey,
        val totalFrames: Int,
        val jankyFrames: Int,
        val frozenFrames: Int,
        val p95FrameMillis: Long,
        val windowMillis: Long,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : PerformancePayload

    data class ResourceProbe(
        val trigger: ProbeTrigger,
        val heapUsedBytes: Long,
        val heapMaxBytes: Long,
        val nativeHeapBytes: Long,
        val availableBytes: Long,
        val lowMemory: Boolean,
        val cpuPercent: Double?,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : PerformancePayload
}
