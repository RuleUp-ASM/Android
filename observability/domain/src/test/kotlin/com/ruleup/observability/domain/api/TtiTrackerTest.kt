package com.ruleup.observability.domain.api

import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.model.TtiOutcome
import com.ruleup.observability.domain.model.TtiPage
import com.ruleup.observability.domain.model.TtiTimeline
import com.ruleup.observability.domain.port.ResourceSampler
import com.ruleup.observability.domain.test.FakeClock
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TtiTrackerTest {
    private object DetailPage : TtiPage {
        override val pageName = "detail"
        override val timelines = listOf(TtiTimeline.API_RESPONSE, TtiTimeline.VIEW_BINDING)
    }

    private object OtherPage : TtiPage {
        override val pageName = "other"
        override val timelines = listOf(TtiTimeline.API_RESPONSE)
    }

    private val clock = FakeClock()
    private val sink = RecordingSink()
    private val sampledTriggers = mutableListOf<ProbeTrigger>()
    private val sampler =
        ResourceSampler { trigger ->
            sampledTriggers += trigger
            probe
        }
    private var probe: PerformancePayload.ResourceProbe? = null
    private val tracker =
        TtiTracker(
            clock = clock,
            observability = testObservability(sink = sink, clock = clock),
            resourceSampler = sampler,
            timeoutMillis = 20_000,
            slowThresholdMillis = 2_000,
        )

    private fun resourceProbe() =
        PerformancePayload.ResourceProbe(
            trigger = ProbeTrigger.TTI_SLOW,
            heapUsedBytes = 1,
            heapMaxBytes = 2,
            nativeHeapBytes = 3,
            availableBytes = 4,
            lowMemory = false,
            cpuPercent = null,
        )

    private fun emitted() = sink.events.mapNotNull { it.payload as? PerformancePayload.Tti }

    @Test
    fun `선언된 단계를 모두 기록하면 COMPLETED`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        tracker.beginPhase(DetailPage, TtiTimeline.API_RESPONSE)
        clock.advanceMillis(300)
        tracker.endPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.beginPhase(DetailPage, TtiTimeline.VIEW_BINDING)
        clock.advanceMillis(50)
        tracker.endPhase(DetailPage, TtiTimeline.VIEW_BINDING)
        tracker.complete(DetailPage)

        val tti = emitted().single()
        assertEquals(TtiOutcome.COMPLETED, tti.outcome)
        assertEquals(350, tti.totalMillis)
        assertEquals(listOf(300L, 50L), tti.phases.map { it.durationMillis })
    }

    @Test
    fun `단계가 빠지면 ABANDONED`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        tracker.beginPhase(DetailPage, TtiTimeline.API_RESPONSE)
        clock.advanceMillis(100)
        tracker.endPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.abandonActive()

        // 호출부가 "이탈했다"고 알려주지 않아도 선언 목록과 대조해 판정한다.
        assertEquals(TtiOutcome.ABANDONED, emitted().single().outcome)
    }

    @Test
    fun `제한 시간을 넘기면 TIMEOUT 이 우선한다`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        clock.advanceMillis(20_001)
        tracker.abandonActive()

        assertEquals(TtiOutcome.TIMEOUT, emitted().single().outcome)
    }

    @Test
    fun `새 화면이 시작되면 이전 세션이 확정된다`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        clock.advanceMillis(100)
        tracker.start(OtherPage, ScreenKey("other"))

        assertEquals(1, emitted().size)
        assertEquals("detail", emitted().single().pageName)
    }

    @Test
    fun `뒤늦은 콜백이 새 세션을 오염시키지 않는다`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        tracker.beginPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.start(OtherPage, ScreenKey("other"))

        // 이전 화면의 응답이 이제 도착 — 새 세션에 기록되면 안 된다.
        tracker.endPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.complete(OtherPage)

        val other = emitted().last()
        assertEquals("other", other.pageName)
        assertTrue(other.phases.isEmpty())
    }

    @Test
    fun `짝이 없는 종료는 무시한다`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        tracker.endPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.complete(DetailPage)

        // 시작 없이 0ms 구간을 만들어내지 않는다.
        assertTrue(emitted().single().phases.isEmpty())
    }

    @Test
    fun `중복 호출은 첫 시각을 유지한다`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        tracker.beginPhase(DetailPage, TtiTimeline.API_RESPONSE)
        clock.advanceMillis(100)
        tracker.beginPhase(DetailPage, TtiTimeline.API_RESPONSE)
        clock.advanceMillis(100)
        tracker.endPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.endPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.complete(DetailPage)

        assertEquals(
            200,
            emitted()
                .single()
                .phases
                .single()
                .durationMillis,
        )
    }

    @Test
    fun `세션이 없으면 아무 일도 없다`() {
        tracker.beginPhase(DetailPage, TtiTimeline.API_RESPONSE)
        tracker.complete(DetailPage)
        tracker.abandonActive()

        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun `느린 화면에는 자원 스냅샷이 따라붙는다`() {
        probe = resourceProbe()
        tracker.start(DetailPage, ScreenKey("detail"))
        clock.advanceMillis(2_500)
        tracker.abandonActive()

        assertEquals(listOf(ProbeTrigger.TTI_SLOW), sampledTriggers)
        assertTrue(sink.payloads.any { it is PerformancePayload.ResourceProbe })
    }

    @Test
    fun `빠른 화면은 자원을 뜨지 않는다`() {
        tracker.start(DetailPage, ScreenKey("detail"))
        clock.advanceMillis(100)
        tracker.abandonActive()

        assertTrue(sampledTriggers.isEmpty())
    }

    @Test
    fun `샘플러가 쓰로틀되면 Tti 만 나간다`() {
        probe = null
        tracker.start(DetailPage, ScreenKey("detail"))
        clock.advanceMillis(2_500)
        tracker.abandonActive()

        assertEquals(listOf(ProbeTrigger.TTI_SLOW), sampledTriggers)
        assertEquals(1, sink.events.size)
    }
}
