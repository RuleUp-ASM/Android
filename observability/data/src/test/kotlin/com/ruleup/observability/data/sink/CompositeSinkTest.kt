package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.test.RecordingSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompositeSinkTest {
    private val event =
        ObsEvent(
            payload = DiagnosticPayload(Severity.WARN, "Tag", "msg"),
            epochMillis = 1L,
            monotonicNanos = 2L,
            profile = BuildProfile.DEV,
            context = ObsContext(currentScreen = null),
        )

    @Test
    fun `자식 하나가 던져도 나머지는 전부 받는다`() {
        val a = RecordingSink("a")
        val boom = RecordingSink("boom").apply { failWith = IllegalStateException("sink down") }
        val c = RecordingSink("c")
        val composite = CompositeSink(listOf(a, boom, c), BuildProfile.PRODUCTION)

        composite.emit(event)

        assertEquals(1, a.events.size)
        assertEquals(1, c.events.size)
    }

    @Test
    fun `프로덕션에서는 자식 실패를 전파하지 않는다`() {
        val boom = RecordingSink().apply { failWith = IllegalStateException("sink down") }
        val composite = CompositeSink(listOf(boom), BuildProfile.PRODUCTION)

        composite.emit(event) // 던지지 않아야 한다 — 로깅이 앱을 죽이면 안 된다.
    }

    @Test
    fun `DEV 에서는 격리를 유지한 채 첫 실패를 재던진다`() {
        val boom = RecordingSink("boom").apply { failWith = IllegalStateException("sink down") }
        val after = RecordingSink("after")
        val composite = CompositeSink(listOf(boom, after), BuildProfile.DEV)

        assertFailsWith<IllegalStateException> { composite.emit(event) }
        // 재던지기가 루프를 끊으면 뒤 자식이 굶는다. 모아뒀다가 루프 종료 후 던져야 한다.
        assertEquals(1, after.events.size)
    }

    @Test
    fun `VirtualMachineError 는 프로덕션에서도 즉시 전파한다`() {
        val boom = RecordingSink().apply { failWith = OutOfMemoryError("heap") }
        val composite = CompositeSink(listOf(boom), BuildProfile.PRODUCTION)

        // 삼키면 무관한 지점에서 죽어 원인 추적이 불가능해진다.
        assertFailsWith<OutOfMemoryError> { composite.emit(event) }
    }

    @Test
    fun `LinkageError 는 삼킨다`() {
        val boom = RecordingSink().apply { failWith = NoClassDefFoundError("com.google.Missing") }
        val composite = CompositeSink(listOf(boom), BuildProfile.PRODUCTION)

        // SDK 누락·R8 설정 같은 패키징 버그로 사용자 앱을 죽일 이유가 없다.
        composite.emit(event)
    }

    @Test
    fun `flush 도 모든 자식에게 전파된다`() {
        val a = RecordingSink("a")
        val b = RecordingSink("b")
        CompositeSink(listOf(a, b), BuildProfile.PRODUCTION).flush()

        assertEquals(1, a.flushCount)
        assertEquals(1, b.flushCount)
    }

    @Test
    fun `채널 필터가 관심 없는 이벤트를 막는다`() {
        val target = RecordingSink()
        val filtered = ChannelFilterSink(setOf(com.ruleup.observability.domain.event.Channel.BUSINESS), target)

        filtered.emit(event) // DIAGNOSTIC

        assertTrue(target.events.isEmpty())
    }

    @Test
    fun `심각도 필터가 하한 미만을 막는다`() {
        val target = RecordingSink()
        val filtered = SeverityFilterSink(Severity.ERROR, target)

        filtered.emit(event) // WARN

        assertTrue(target.events.isEmpty())
    }
}
