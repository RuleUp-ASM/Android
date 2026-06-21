package com.ruleup.verification.data.db

import com.ruleup.verification.domain.entity.GeofenceTransitionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignalEntityMapperTest {
    @Test
    fun `geofence 엔티티가 도메인 이벤트로 정확히 매핑된다`() {
        val entity =
            GeofenceTransitionEntity(
                requestId = "member-1",
                transition = "DWELL",
                lat = 37.49,
                lng = 127.02,
                accuracy = 10.5f,
                isMock = true,
                occurredAt = 123_456L,
            )

        val event = entity.toDomain()

        assertEquals("member-1", event.requestId)
        assertEquals(GeofenceTransitionType.DWELL, event.transition)
        assertEquals(123_456L, event.at)
        assertEquals(37.49, event.lat)
        // isMock 은 처음부터 보존·전송(명세 §7).
        assertTrue(event.isMock)
    }

    @Test
    fun `location 샘플이 도메인 포인트로 매핑된다`() {
        val entity =
            LocationSampleEntity(
                lat = 37.0,
                lng = 127.0,
                accuracy = 8f,
                isMock = false,
                occurredAt = 999L,
            )

        val point = entity.toDomain()

        assertEquals(37.0, point.lat)
        assertEquals(999L, point.at)
        assertEquals(false, point.isMock)
    }

    @Test
    fun `미인식 transition 문자열은 ENTER 로 떨어진다`() {
        val entity =
            GeofenceTransitionEntity(
                requestId = "m",
                transition = "UNKNOWN_FUTURE",
                lat = 0.0,
                lng = 0.0,
                accuracy = 0f,
                isMock = false,
                occurredAt = 0L,
            )

        assertEquals(GeofenceTransitionType.ENTER, entity.toDomain().transition)
    }
}
