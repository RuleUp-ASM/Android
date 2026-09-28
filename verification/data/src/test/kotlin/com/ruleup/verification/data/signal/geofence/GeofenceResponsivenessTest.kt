package com.ruleup.verification.data.signal.geofence

import kotlin.test.Test
import kotlin.test.assertEquals

class GeofenceResponsivenessTest {
    @Test
    fun `체류 목표가 기본치보다 길면 기본치를 쓴다`() {
        // 0 을 쓰면 OS 배칭이 꺼져 위치 하드웨어가 상시 깨어 있게 된다(#357).
        assertEquals(DEFAULT_GEOFENCE_RESPONSIVENESS_MS, geofenceResponsivenessFor(30 * 60_000))
    }

    @Test
    fun `체류 목표가 기본치보다 짧으면 거기에 맞춘다`() {
        assertEquals(3 * 60_000, geofenceResponsivenessFor(3 * 60_000))
    }

    @Test
    fun `체류 목표가 없는 펜스는 기본치를 쓴다`() {
        assertEquals(DEFAULT_GEOFENCE_RESPONSIVENESS_MS, geofenceResponsivenessFor(0))
    }
}
