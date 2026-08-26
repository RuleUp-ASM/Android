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
        // 체류 임계보다 통지가 늦게 잡히면 짧은 체류 방이 성공하고도 DWELL 을 못 받는다.
        assertEquals(3 * 60_000, geofenceResponsivenessFor(3 * 60_000))
    }

    @Test
    fun `체류 목표가 없는 펜스는 기본치를 쓴다`() {
        // DWELL 을 쏘지 않는 펜스(진입·이탈만)라 깎을 이유가 없다 — 0 으로 접으면 배칭만 잃는다.
        assertEquals(DEFAULT_GEOFENCE_RESPONSIVENESS_MS, geofenceResponsivenessFor(0))
    }
}
