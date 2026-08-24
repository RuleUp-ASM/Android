package com.ruleup.verification.data.db

import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.geofence.GeofenceTransitionEntity
import com.ruleup.verification.data.db.geofence.LocationSampleEntity
import com.ruleup.verification.data.db.health.HealthReadingEntity
import com.ruleup.verification.data.db.health.SleepSegmentEntity
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import com.ruleup.verification.domain.entity.HealthDeviceType
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.RecordingMethod
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
                accuracy = 10.5f,
                isMock = true,
                occurredAt = 123_456L,
                observedElapsedMillis = 98_765L,
            )

        val event = entity.toDomain()

        assertEquals("member-1", event.requestId)
        assertEquals(GeofenceTransitionType.DWELL, event.transition)
        assertEquals(123_456L, event.observedAt)
        // 벽시계와 monotonic 을 함께 올려야 서버가 시각 조작을 대조할 수 있다(전송 스펙 §6.4).
        assertEquals(98_765L, event.observedElapsedMillis)
        assertEquals(10.5f, event.accuracy)
        assertTrue(event.isMock == true)
    }

    @Test
    fun `위치 없는 전이는 정확도와 mock 여부를 지어내지 않는다`() {
        val entity =
            GeofenceTransitionEntity(
                requestId = "member-1",
                transition = "ENTER",
                occurredAt = 123_456L,
                observedElapsedMillis = 98_765L,
            )

        val event = entity.toDomain()

        // 0m·"mock 아님"으로 접으면 없던 사실이 판정에 들어간다 — 모르는 값은 null 로 올린다.
        assertEquals(null, event.accuracy)
        assertEquals(null, event.isMock)
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
    fun `health 읽기 엔티티가 도메인 reading·origin 으로 매핑된다`() {
        val entity =
            HealthReadingEntity(
                metric = "DISTANCE",
                value = 5.2,
                unit = "km",
                exerciseType = "RUNNING",
                dataOrigin = "com.sec.android.app.shealth",
                recordingMethod = "AUTO",
                deviceType = "WATCH",
                date = "2026-06-24",
                occurredAt = 123L,
            )

        val reading = entity.toDomain()

        assertEquals(HealthMetric.DISTANCE, reading.metric)
        assertEquals(5.2, reading.value)
        assertEquals("RUNNING", reading.exerciseType)
        assertEquals("com.sec.android.app.shealth", reading.origin.dataOrigin)
        assertEquals(RecordingMethod.AUTO, reading.origin.recordingMethod)
        assertEquals(HealthDeviceType.WATCH, reading.origin.deviceType)
        assertEquals(123L, reading.at)
    }

    @Test
    fun `미인식 recordingMethod·deviceType 은 UNKNOWN 으로 떨어진다`() {
        val entity =
            HealthReadingEntity(
                metric = "STEPS",
                value = 1000.0,
                unit = "count",
                exerciseType = null,
                dataOrigin = "com.unknown.app",
                recordingMethod = "FUTURE_VALUE",
                deviceType = "FUTURE_DEVICE",
                date = "2026-06-24",
                occurredAt = 0L,
            )

        val origin = entity.toDomain().origin
        assertEquals(RecordingMethod.UNKNOWN, origin.recordingMethod)
        assertEquals(HealthDeviceType.UNKNOWN, origin.deviceType)
    }

    @Test
    fun `sleep 세그먼트 엔티티가 도메인으로 매핑된다`() {
        val entity =
            SleepSegmentEntity(
                startAt = 100L,
                endAt = 200L,
                status = "DEEP",
                occurredAt = 200L,
            )

        val segment = entity.toDomain()
        assertEquals(100L, segment.startAt)
        assertEquals(200L, segment.endAt)
        assertEquals("DEEP", segment.status)
    }

    @Test
    fun `미인식 transition 문자열은 ENTER 로 떨어진다`() {
        val entity =
            GeofenceTransitionEntity(
                requestId = "m",
                transition = "UNKNOWN_FUTURE",
                accuracy = 0f,
                isMock = false,
                occurredAt = 0L,
                observedElapsedMillis = 0L,
            )

        assertEquals(GeofenceTransitionType.ENTER, entity.toDomain().transition)
    }
}
