package com.ruleup.remoteconfig.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessConfigTest {
    @Test
    fun `실행 중에 새 실험 설정을 받아도 노출에는 처음 적용한 안을 쓴다`() {
        val process = ProcessConfig()
        val first = ConfigSnapshot(100, "A", ExperimentAssignment("run-1", "a", "A"))
        val next = ConfigSnapshot(200, "C", ExperimentAssignment("run-2", "c", "C"))
        process.stage(KEY, first)

        assertEquals(first, process.pin(KEY, 101))

        process.stage(KEY, next)

        assertEquals(first, process.pin(KEY, 201))
        assertEquals(first.assignment, process.applied(KEY)?.assignment)
        val restarted = ProcessConfig()
        restarted.stage(KEY, next)
        assertEquals(next, restarted.pin(KEY, 201))
    }

    @Test
    fun `같은 실행의 최초 배정은 OFF를 거쳐도 유지한다`() {
        val first = ConfigSnapshot(100, "A", ExperimentAssignment("run-1", "a", "A"))
        val off = ConfigSnapshot(200).preserveAssignment(first)
        val drift = ConfigSnapshot(300, "C", ExperimentAssignment("run-1", "c", "C"))
        val process = ProcessConfig()
        process.stage(KEY, first)
        process.pin(KEY, 101)
        process.stage(KEY, off)

        assertEquals("A", process.pin(KEY, 201)?.value)
        val restarted = ProcessConfig()
        restarted.stage(KEY, off)
        assertNull(restarted.pin(KEY, 201)?.value)
        assertEquals(first.assignment, drift.preserveAssignment(off).assignment)
    }

    @Test
    fun `만료된 캐시나 첫 읽기에 없던 값은 실행 중 뒤늦게 적용하지 않는다`() {
        val day = 24 * 60 * 60 * 1_000L
        val snapshot = ConfigSnapshot(0, "A")
        val expired = ProcessConfig()
        expired.stage(KEY, snapshot)
        val empty = ProcessConfig()

        assertTrue(snapshot.isFresh(day))
        assertNull(expired.pin(KEY, day + 1))
        assertNull(empty.pin(KEY, 1))

        empty.stage(KEY, snapshot)
        expired.stage(KEY, ConfigSnapshot(day + 2, "C"))
        assertNull(empty.pin(KEY, 2))
        assertNull(expired.pin(KEY, day + 2))
    }

    @Test
    fun `전환은 첫 배정부터 7일이 되기 전까지만 귀속한다`() {
        val enrolledAt = 10_000L
        val cutoff = enrolledAt + 7 * 24 * 60 * 60 * 1_000L
        val enrollment = ExperimentEnrollment(ExperimentAssignment("run-1", "a", "A"), enrolledAt)

        assertFalse(enrollment.isWithinConversionWindow(enrolledAt - 1))
        assertTrue(enrollment.isWithinConversionWindow(enrolledAt))
        assertTrue(enrollment.isWithinConversionWindow(cutoff - 1))
        assertFalse(enrollment.isWithinConversionWindow(cutoff))
    }

    private companion object {
        const val KEY = "watcher_invite_ui"
    }
}
