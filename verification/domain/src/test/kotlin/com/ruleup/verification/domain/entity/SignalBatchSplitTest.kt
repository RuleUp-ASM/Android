package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignalBatchSplitTest {
    @Test
    fun `신호 목록이 아니라 신호 안의 이벤트를 가른다`() {
        // 하루치 걸음은 HEALTH 신호 한 건에 readings 수천 개로 들어온다. 신호 단위로만 나누면
        // 그 한 건이 통째로 남아 상한을 다시 넘고, 분할이 영영 수렴하지 않는다.
        val batch = batchOf(health(readings = 5))

        val (head, tail) = assertNotNull(batch.split())

        assertEquals(3, (head.signals.single() as VerificationSignal.Health).readings.size)
        assertEquals(2, (tail.signals.single() as VerificationSignal.Health).readings.size)
    }

    @Test
    fun `신호 레벨 값은 양쪽 조각이 그대로 물려받는다`() {
        // date·metric 이 빠진 조각은 서버가 어느 날 무슨 지표인지 알 수 없어 통째로 버려진다.
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
        // WAKE 는 목록이 아니라 당일 가공값 한 벌이다. 뒤쪽으로 복제되면 같은 기상 시각이 두 번 간다.
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
        // 배치 키는 버퍼를 synced 로 표시하는 단위지 전송 단위가 아니다 — 조각마다 키가 달라지면
        // 어느 조각이 나갔는지와 무관하게 버퍼 표시가 어긋난다.
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
