package com.ruleup.verification.data.signal.geofence

import com.ruleup.verification.domain.entity.GeofenceTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeofenceReconcileTest {
    @Test
    fun `desired 에서 빠진 requestId 만 제거 대상이다`() {
        val previous = setOf("a", "b", "c")
        val targets = listOf(target("b"), target("d"))

        val toRemove = GeofenceReconcile.toRemove(previous, targets)

        assertEquals(setOf("a", "c"), toRemove.toSet())
    }

    @Test
    fun `목표가 비면 직전 전체가 제거 대상이다`() {
        val previous = setOf("a", "b")

        val toRemove = GeofenceReconcile.toRemove(previous, emptyList())

        assertEquals(setOf("a", "b"), toRemove.toSet())
    }

    @Test
    fun `직전이 비면 제거 대상이 없다 - 재부팅 후 전체 재등록`() {
        val toRemove = GeofenceReconcile.toRemove(emptySet(), listOf(target("a"), target("b")))

        assertTrue(toRemove.isEmpty())
    }

    private fun target(id: String): GeofenceTarget = GeofenceTarget(requestId = id, lat = 0.0, lng = 0.0, radiusM = 100f, dwellMinutes = 60)
}
