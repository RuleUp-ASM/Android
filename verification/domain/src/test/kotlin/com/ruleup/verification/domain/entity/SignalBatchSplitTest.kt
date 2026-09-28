package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignalBatchSplitTest {
    @Test
    fun `신호 목록이 아니라 신호 안의 이벤트를 가른다`() {
        val batch = batchOf(health(readings = 5))

        val (head, tail) = assertNotNull(batch.split())

        assertEquals(3, (head.signals.single() as VerificationSignal.Health).readings.size)
        assertEquals(2, (tail.signals.single() as VerificationSignal.Health).readings.size)
    }

    @Test
    fun `신호 레벨 값은 양쪽 조각이 그대로 물려받는다`() {
        val batch = batchOf(health(readings = 4))

        val (head, tail) = assertNotNull(batch.split())

        listOf(head, tail).forEach {
            val signal = it.signals.single() as VerificationSignal.Health
            assertEquals("2026-06-24", signal.date)
            assertEquals(HealthMetric.STEPS, signal.metric)
        }
    }

    @Test
    fun `가를 수 없는 신호는 앞쪽에 통째로 남는다`() {
        val wake = VerificationSignal.Wake(firstUnlock = 1L, firstScreenOn = null, deviceSecure = true)
        val batch = batchOf(wake, health(readings = 2))

        val (head, tail) = assertNotNull(batch.split())

        assertTrue(head.signals.any { it is VerificationSignal.Wake })
        assertTrue(tail.signals.none { it is VerificationSignal.Wake })
    }

    @Test
    fun `모든 신호가 한 건뿐이면 더 못 쪼갠다`() {
        // 여기서 null 을 안 주면 호출부가 같은 요청을 무한히 되풀이한다.
        val batch = batchOf(health(readings = 1))

        assertNull(batch.split())
    }

    @Test
    fun `빈 배치도 더 못 쪼갠다`() {
        assertNull(SignalBatch(collectedAt = COLLECTED_AT, signals = emptyList()).split())
    }

    @Test
    fun `쪼갠 조각은 같은 배치 키를 쓴다`() {
        val (head, tail) = assertNotNull(batchOf(health(readings = 2)).split())

        assertEquals(COLLECTED_AT, head.collectedAt)
        assertEquals(COLLECTED_AT, tail.collectedAt)
    }

    private fun batchOf(vararg signals: VerificationSignal): SignalBatch =
        SignalBatch(collectedAt = COLLECTED_AT, signals = signals.toList())

    private fun health(readings: Int): VerificationSignal.Health =
        VerificationSignal.Health(
            date = "2026-06-24",
            metric = HealthMetric.STEPS,
            readings =
                (1..readings).map {
                    HealthReading(
                        recordId = "hc-$it",
                        value = it.toDouble(),
                        startTime = it.toLong(),
                        endTime = it.toLong(),
                        recordingMethod = RecordingMethod.AUTO,
                        originPackage = "com.sec.android.app.shealth",
                    )
                },
        )

    private companion object {
        const val COLLECTED_AT = "2026-06-24T00:00:00Z"
    }
}
