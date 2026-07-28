package com.ruleup.observability.data.policy

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RuntimePolicyTest {
    private fun policy(config: PolicyConfig) = RuntimePolicy(config)

    @Test
    fun `채널 floor 는 서로 독립이다`() {
        // 프로덕션의 전형적 설정 — 진단만 WARN 으로 올린다.
        val p = policy(PolicyConfig.of(channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN)))

        assertFalse(p.isEnabled(Channel.DIAGNOSTIC, Severity.INFO, null))
        // 비즈니스·성능 페이로드는 전부 INFO 다. 진단 floor 가 여기까지 적용되면 지표가 통째로 사라진다.
        assertTrue(p.isEnabled(Channel.BUSINESS, Severity.INFO, null))
        assertTrue(p.isEnabled(Channel.PERFORMANCE, Severity.INFO, null))
    }

    @Test
    fun `설정에 없는 채널은 제한 없음으로 본다`() {
        val p = policy(PolicyConfig.of(channelFloors = emptyMap()))

        assertTrue(p.isEnabled(Channel.BUSINESS, Severity.VERBOSE, null))
    }

    @Test
    fun `태그 오버라이드가 채널 floor 를 대체한다`() {
        val p =
            policy(
                PolicyConfig.of(
                    channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN),
                    tagOverrides = mapOf("Sync" to Severity.VERBOSE),
                ),
            )

        assertTrue(p.isEnabled(Channel.DIAGNOSTIC, Severity.DEBUG, "Sync"))
        assertFalse(p.isEnabled(Channel.DIAGNOSTIC, Severity.DEBUG, "Other"))
    }

    @Test
    fun `꺼진 채널은 심각도와 무관하게 막힌다`() {
        val p = policy(PolicyConfig.of(channelFloors = emptyMap(), disabledChannels = setOf(Channel.BUSINESS)))

        assertFalse(p.isEnabled(Channel.BUSINESS, Severity.ERROR, null))
    }

    @Test
    fun `config 는 isEnabled 가 쓰는 스냅샷과 같은 참조다`() {
        val initial = PolicyConfig.of(channelFloors = mapOf(Channel.DIAGNOSTIC to Severity.WARN))
        val p = policy(initial)

        // 인스펙터가 보는 설정과 게이트가 실제로 쓴 설정이 갈라지면 안 된다.
        assertSame(initial, p.config())

        p.update { it.withChannelFloor(Channel.DIAGNOSTIC, Severity.VERBOSE) }
        assertNotSame(initial, p.config())
        assertTrue(p.isEnabled(Channel.DIAGNOSTIC, Severity.VERBOSE, null))
    }

    @Test
    fun `동시 갱신이 유실되지 않는다`() {
        val p = policy(PolicyConfig.of(channelFloors = emptyMap()))
        val threads =
            (1..8).map { index ->
                Thread { repeat(50) { p.update { c -> c.withExtra("k$index", "$it") } } }
            }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        // read-modify-write 를 CAS 로 하지 않으면 일부 키가 통째로 사라진다.
        assertEquals(8, p.config().extras.size)
    }
}
