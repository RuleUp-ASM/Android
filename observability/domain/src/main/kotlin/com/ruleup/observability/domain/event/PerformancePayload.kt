package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.ProbeTrigger
import com.ruleup.observability.domain.model.ScreenKey

sealed interface PerformancePayload : ObsPayload {
    override val channel: Channel get() = Channel.PERFORMANCE

    /**
     * 화면이 쓸 수 있게 되기까지의 소요. 구간 어휘는 `:tti:domain` 이 갖고 여기는 **이름과 밀리초만**
     * 받는다 — 관측 모듈이 TTI 의 단계 정의를 알면 구간이 늘 때마다 여기까지 고쳐야 한다.
     *
     * [spans] 키는 구간 이름이고 값은 그 구간의 길이다. [totalMillis] 는 그 합이지 처음과 끝의
     * 차이가 아니다.
     */
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
