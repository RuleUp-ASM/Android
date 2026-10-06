package com.ruleup.android_ruleup.tti

import android.app.Application
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.test.RecordingSink
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiSpan
import com.ruleup.tti.domain.TtiTimeline
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 저장된 구간 시간이 전송 시점의 시간으로 바뀌면 콘솔의 TTI 통계가 왜곡된다. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = Application::class,
    shadows = [TtiFirebasePerformanceShadow::class, TtiTraceShadow::class],
)
class ObservabilityTtiShooterTest {
    private val sink = RecordingSink()
    private val shooter = ObservabilityTtiShooter(testObservability(sink = sink))

    @Before
    fun resetFirebase() {
        TtiFirebasePerformanceShadow.traces.clear()
        TtiFirebasePerformanceShadow.failure = null
    }

    @Test
    fun `완성된 기록은 화면별로 원래 구간 시간과 합계를 두 관측 경로에 보낸다`() =
        runTest {
            val records = listOf(record("home"), record("challenge_detail"))

            shooter.shoot(records)

            assertEquals(2, TtiFirebasePerformanceShadow.traces.size)
            records.zip(TtiFirebasePerformanceShadow.traces).forEach { (record, entry) ->
                val (name, trace) = entry
                // 하나의 trace 에 섞이면 콘솔에서 화면별 TTI 를 가를 수 없다(#564).
                assertEquals("tti_${record.pageName}", name)
                assertEquals(mapOf("page_name" to record.pageName), trace.attributes)
                assertEquals(
                    mapOf(
                        "total_millis" to 800L,
                        "view_create_millis" to 100L,
                        "backend_millis" to 500L,
                        "view_binding_millis" to 200L,
                        "big_part_loading_millis" to 0L,
                    ),
                    trace.metrics,
                )
                assertEquals(1, trace.starts)
                assertEquals(1, trace.stops)
            }
            assertEquals(
                records.map {
                    PerformancePayload.Tti(
                        pageName = it.pageName,
                        totalMillis = 800L,
                        spans = it.spans.mapKeys { (timeline, _) -> timeline.name }.mapValues { (_, span) -> span.durationMillis!! },
                    )
                },
                sink.payloads,
            )
        }

    @Test
    fun `어느 구간이든 없거나 미완성이면 전송하지 않는다`() =
        runTest {
            val complete = record("home")
            val incomplete =
                TtiTimeline.entries.flatMap { timeline ->
                    listOf(
                        complete.copy(spans = complete.spans - timeline),
                        complete.copy(spans = complete.spans + (timeline to TtiSpan(10L))),
                    )
                }

            shooter.shoot(incomplete + complete)

            assertEquals(1, TtiFirebasePerformanceShadow.traces.size)
            assertEquals(1, sink.events.size)
        }

    @Test
    fun `빈 목록이면 Firebase 에 접근하지 않는다`() =
        runTest {
            TtiFirebasePerformanceShadow.failure = IllegalStateException("Firebase 미초기화")

            shooter.shoot(emptyList())

            assertTrue(TtiFirebasePerformanceShadow.traces.isEmpty())
            assertTrue(sink.events.isEmpty())
        }

    @Test
    fun `Firebase 호출이 실패하면 기록을 재시도할 수 있도록 실패를 전달한다`() =
        runTest {
            TtiFirebasePerformanceShadow.failure = IllegalStateException("Firebase 미초기화")

            assertFailsWith<IllegalStateException> { shooter.shoot(listOf(record("home"))) }

            assertTrue(sink.events.isEmpty())
        }

    @Test
    fun `화면 이름이 Firebase 제한을 넘으면 속성만 줄이고 기존 이벤트는 보존한다`() =
        runTest {
            val pageName = "a".repeat(101)

            shooter.shoot(listOf(record(pageName)))

            assertEquals(
                "a".repeat(100),
                TtiFirebasePerformanceShadow.traces
                    .single()
                    .second.attributes["page_name"],
            )
            assertEquals(pageName, (sink.single.payload as PerformancePayload.Tti).pageName)
            assertEquals(100, TtiFirebasePerformanceShadow.traces.single().first.length)
        }

    @Test
    fun `trace 이름에 Firebase 가 막는 문자가 있으면 밑줄로 바꾼다`() {
        // 선행 밑줄이나 공백이 남으면 Firebase 가 trace 를 버려 그 화면만 조용히 빠진다.
        assertEquals("tti_challenge_detail", ttiTraceName("challenge/detail"))
        assertEquals("tti_me", ttiTraceName("_me "))
        assertEquals("tti_unknown", ttiTraceName(""))
    }

    private fun record(pageName: String) =
        TtiRecord(
            tti = Tti(),
            pageName = pageName,
            createdAt = 1L,
            spans =
                mapOf(
                    TtiTimeline.VIEW_CREATE to TtiSpan(10L, 110L),
                    TtiTimeline.BACKEND to TtiSpan(110L, 610L),
                    TtiTimeline.VIEW_BINDING to TtiSpan(610L, 810L),
                    TtiTimeline.BIG_PART_LOADING to TtiSpan(810L, 810L),
                ),
        )
}

@Implements(FirebasePerformance::class)
class TtiFirebasePerformanceShadow {
    @Implementation
    fun newTrace(name: String): Trace {
        val trace = Shadow.newInstanceOf(Trace::class.java)
        traces += name to Shadow.extract<TtiTraceShadow>(trace)
        return trace
    }

    companion object {
        val traces = mutableListOf<Pair<String, TtiTraceShadow>>()
        var failure: RuntimeException? = null

        @JvmStatic
        @Implementation
        fun getInstance(): FirebasePerformance {
            failure?.let { throw it }
            return Shadow.newInstanceOf(FirebasePerformance::class.java)
        }
    }
}

@Implements(Trace::class)
class TtiTraceShadow {
    val attributes = mutableMapOf<String, String>()
    val metrics = mutableMapOf<String, Long>()
    var starts = 0
    var stops = 0

    @Implementation
    fun start() {
        starts++
    }

    @Implementation
    fun putAttribute(
        name: String,
        value: String,
    ) {
        check(starts == 1 && stops == 0)
        attributes[name] = value
    }

    @Implementation
    fun putMetric(
        name: String,
        value: Long,
    ) {
        check(starts == 1 && stops == 0)
        metrics[name] = value
    }

    @Implementation
    fun stop() {
        stops++
    }
}
