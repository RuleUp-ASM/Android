package com.ruleup.observability.data.policy

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PolicyConfigTest {
    @Test
    fun `생성 후 원본 맵을 수정해도 스냅샷은 그대로다`() {
        val source = mutableMapOf(Channel.DIAGNOSTIC to Severity.WARN)
        val config = PolicyConfig.of(channelFloors = source)

        source[Channel.BUSINESS] = Severity.ERROR

        // 방어적 복사가 없으면 AtomicReference 스왑의 전제(참조 교체 없이는 내용이 안 바뀜)가 무너진다.
        assertEquals(1, config.channelFloors.size)
    }

    @Test
    fun `변환은 새 스냅샷을 만들고 원본을 건드리지 않는다`() {
        val base = PolicyConfig.of(channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN))

        val next = base.withChannelFloor(Channel.BUSINESS, Severity.ERROR)

        assertEquals(1, base.channelFloors.size)
        assertEquals(2, next.channelFloors.size)
    }

    @Test
    fun `태그 오버라이드는 null 로 해제된다`() {
        val base = PolicyConfig.of(channelFloors = emptyMap()).withTagOverride("Sync", Severity.VERBOSE)

        assertEquals(1, base.tagOverrides.size)
        assertTrue(base.withTagOverride("Sync", null).tagOverrides.isEmpty())
    }

    @Test
    fun `채널 on off 토글`() {
        val off = PolicyConfig.of(channelFloors = emptyMap()).withChannel(Channel.BUSINESS, enabled = false)
        assertTrue(Channel.BUSINESS in off.disabledChannels)
        assertTrue(off.withChannel(Channel.BUSINESS, enabled = true).disabledChannels.isEmpty())
    }

    @Test
    fun `요약에 채널별 상태가 드러난다`() {
        val config =
            PolicyConfig.of(
                channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN),
                disabledChannels = setOf(Channel.PERFORMANCE),
            )

        val summary = config.summary()
        assertTrue("DIAG=WARN" in summary, summary)
        assertTrue("PERF=OFF" in summary, summary)
    }

    @Test
    fun `같은 내용이면 동등하다`() {
        val a = PolicyConfig.of(channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN))
        val b = PolicyConfig.of(channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN))
        assertEquals(a, b)
        assertSame(a.channelFloors[Channel.DIAGNOSTIC], b.channelFloors[Channel.DIAGNOSTIC])
    }
}
