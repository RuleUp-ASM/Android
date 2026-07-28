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

/**
 * JankStats 프레임 데이터를 성능 채널의 [PerformancePayload.JankWindow] 로 집계한다.
 *
 * **프레임마다 이벤트를 내지 않는다.** 60fps 에서 프레임 콜백은 초당 60회고, 그중 janky 만 골라도
 * 스크롤 한 번에 수십 건이 된다. [WINDOW_MILLIS] 창으로 묶어 창당 한 건만 내보낸다 —
 * 개별 느린 프레임보다 "이 화면에서 창 안에 몇 % 가 jank 였나"가 실제로 쓰는 지표다.
 *
 * ## 유휴 구간이 창을 늘리지 않는다
 * 프레임 콜백은 **화면이 실제로 그려질 때만** 온다. 사용자가 가만히 있으면 콜백도 멈추므로,
 * 도착만으로 창을 닫으면 유휴 시간이 통째로 창에 포함된다(실측에서 5초 창이 214초로 늘어났다).
 * 그러면 jank 비율의 분모가 무의미해진다. 그래서 프레임 간격이 [IDLE_GAP_MILLIS] 를 넘으면
 * **직전 프레임 시점에서 창을 닫고** 새 창을 연다.
 *
 * ## 화면이 바뀌면 창을 끊는다
 * 창은 [PerformancePayload.JankWindow.screen] 에 **닫히는 순간의 화면**을 붙인다. 창이 화면
 * 경계를 넘으면 홈에서 난 jank 가 상세 화면에 귀속되어, 화면별 성능 비교라는 목적 자체가 깨진다.
 * 그래서 네비게이션이 [onScreenChanged] 로 창을 닫아준다 — TTI 와 같은 경계다.
 *
 * 창 안에 jank 가 없으면 [PerformancePayload.JankWindow] 를 내보내지 않는다. 정상 구간까지
 * 기록하면 이벤트 수만 늘고 분석에서 걸러야 한다.
 *
 * 대신 **자원 스냅샷은 양쪽 다** 뜬다. jank 가 있으면 [ProbeTrigger.JANK_DETECTED], 없으면
 * [ProbeTrigger.PERIODIC] 로 — 기준선이 없으면 *"jank 때 힙 120MB"* 를 해석할 수 없다.
 * 별도 타이머가 필요 없다. 이 창 루프가 이미 규칙적으로 돌기 때문이다.
 * 실제 방출 여부는 [ResourceSampler] 의 쓰로틀이 정한다.
 *
 * 스레드: JankStats 콜백은 메인 스레드에서만 온다. 그래서 동기화 없이 누적한다.
 */
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

        // 창 하나(5초 × 60fps ≈ 300개) 분량. p95 는 janky 만이 아니라 전 프레임 기준이어야 한다.
        private val durationsMillis = ArrayList<Long>(INITIAL_SAMPLES)

        /**
         * 화면 전환 직전에 호출한다. 진행 중인 창을 **이전 화면 기준으로** 확정한다.
         *
         * 반드시 화면 컨텍스트를 갱신하기 **전에** 불러야 한다. 갱신 뒤에 부르면 창이 새 화면에
         * 귀속되어 고치려던 문제가 그대로 남는다.
         */
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
