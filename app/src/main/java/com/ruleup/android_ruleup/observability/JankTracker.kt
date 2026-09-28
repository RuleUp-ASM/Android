package com.ruleup.android_ruleup.observability

import androidx.metrics.performance.FrameData
import com.ruleup.observability.data.context.ScreenContextHolder
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.port.ResourceSampler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/** JankStats 프레임 데이터를 성능 채널의 [PerformancePayload.JankWindow] 로 집계한다. */
@Singleton
class JankTracker
    @Inject
    constructor(
        private val observability: Observability,
        private val contextHolder: ScreenContextHolder,
        private val resourceSampler: ResourceSampler,
    ) {
        private var windowStartNanos = 0L
        private var lastFrameNanos = 0L
        private var total = 0
        private var janky = 0
        private var frozen = 0

        // 창 하나(5초 × 60fps ≈ 300개) 분량.
        private val durationsMillis = ArrayList<Long>(INITIAL_SAMPLES)

        /** 화면 전환 직전에 호출한다. */
        fun onScreenChanged() {
            if (total > 0) closeWindow((lastFrameNanos - windowStartNanos) / NANOS_PER_MILLI)
            windowStartNanos = 0L
            lastFrameNanos = 0L
        }

        fun onFrame(frame: FrameData) {
            val startNanos = frame.frameStartNanos
            if (windowStartNanos == 0L) {
                windowStartNanos = startNanos
                lastFrameNanos = startNanos
            }

            if (startNanos - lastFrameNanos >= IDLE_GAP_MILLIS * NANOS_PER_MILLI) {
                closeWindow((lastFrameNanos - windowStartNanos) / NANOS_PER_MILLI)
                windowStartNanos = startNanos
            }
            lastFrameNanos = startNanos

            total++
            val millis = (frame.frameDurationUiNanos / NANOS_PER_MILLI.toDouble()).roundToLong()
            durationsMillis += millis
            if (frame.isJank) {
                janky++
                if (millis >= FROZEN_THRESHOLD_MILLIS) frozen++
            }

            if (startNanos - windowStartNanos >= WINDOW_MILLIS * NANOS_PER_MILLI) {
                closeWindow((startNanos - windowStartNanos) / NANOS_PER_MILLI)
                windowStartNanos = startNanos
            }
        }

        private fun closeWindow(windowMillis: Long) {
            if (total == 0) return
            emitResourceProbe(if (janky == 0) ProbeTrigger.PERIODIC else ProbeTrigger.JANK_DETECTED)
            if (janky > 0) {
                val screen = contextHolder.current().currentScreen ?: ScreenKey(UNKNOWN_SCREEN)
                val sorted = durationsMillis.sorted()
                val p95 = sorted[(sorted.size * P95 / 100).coerceAtMost(sorted.size - 1)]
                observability.log(Channel.PERFORMANCE) {
                    PerformancePayload.JankWindow(
                        screen = screen,
                        totalFrames = total,
                        jankyFrames = janky,
                        frozenFrames = frozen,
                        p95FrameMillis = p95,
                        windowMillis = windowMillis,
                    )
                }
            }
            reset()
        }

        private fun emitResourceProbe(trigger: ProbeTrigger) {
            val probe = resourceSampler.sample(trigger) ?: return
            observability.log(Channel.PERFORMANCE) { probe }
        }

        private fun reset() {
            total = 0
            janky = 0
            frozen = 0
            durationsMillis.clear()
        }

        private companion object {
            const val WINDOW_MILLIS = 5_000L

            /** 이보다 오래 프레임이 없으면 유휴로 보고 창을 끊는다. */
            const val IDLE_GAP_MILLIS = 1_000L
            const val NANOS_PER_MILLI = 1_000_000L

            /** Android Vitals 의 frozen frame 기준. */
            const val FROZEN_THRESHOLD_MILLIS = 700L
            const val P95 = 95
            const val INITIAL_SAMPLES = 320
            const val UNKNOWN_SCREEN = "unknown"
        }
    }
