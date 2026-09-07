package com.ruleup.verification.data.db

import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.geofence.GeofenceTransitionEntity
import com.ruleup.verification.data.db.geofence.LocationSampleEntity
import com.ruleup.verification.data.db.health.HealthReadingEntity
import com.ruleup.verification.data.db.health.SleepSessionEntity
import com.ruleup.verification.domain.entity.GeofenceTransitionType
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
    fun `health 읽기 엔티티가 도메인 reading 으로 매핑된다`() {
        val entity =
            HealthReadingEntity(
                recordId = "hc-1",
                metric = HealthMetric.DISTANCE,
                value = 5.2,
                startTime = 100L,
                endTime = 123L,
                originPackage = "com.sec.android.app.shealth",
                recordingMethod = RecordingMethod.AUTO,
                date = "2026-06-24",
                occurredAt = 123L,
            )

        val reading = entity.toDomain()

        // recordId 는 하루치를 재전송해도 중복이 안 쌓이는 유일한 근거다.
        assertEquals("hc-1", reading.recordId)
        assertEquals(5.2, reading.value)
        // 인증 창이 하루보다 좁은 챌린지가 있어 날짜만으로는 귀속이 안 된다.
        assertEquals(100L, reading.startTime)
        assertEquals(123L, reading.endTime)
        assertEquals("com.sec.android.app.shealth", reading.originPackage)
        assertEquals(RecordingMethod.AUTO, reading.recordingMethod)
    }

    @Test
    fun `sleep 세션 엔티티가 도메인으로 매핑된다`() {
        val entity =
            SleepSessionEntity(
                recordId = "sleep-1",
                startAt = 100L,
                endAt = 200L,
                durationMillis = 100L,
                sleepMillis = 80L,
                observedElapsedMillis = 55L,
                originPackage = "com.sec.android.app.shealth",
                recordingMethod = RecordingMethod.AUTO,
                occurredAt = 200L,
            )

        val session = entity.toDomain()
        assertEquals("sleep-1", session.recordId)
        assertEquals(100L, session.start)
        assertEquals(200L, session.end)
        assertEquals(100L, session.durationMillis)
        assertEquals(80L, session.sleepMillis)
        assertEquals(55L, session.observedElapsedMillis)
    }

    @Test
    fun `stage 를 못 받은 세션은 실수면 시간이 null 로 남는다`() {
        val entity =
            SleepSessionEntity(
                recordId = "sleep-2",
                startAt = 0L,
                endAt = 100L,
                durationMillis = 100L,
                sleepMillis = null,
                observedElapsedMillis = 0L,
                originPackage = "com.unknown.app",
                recordingMethod = RecordingMethod.UNKNOWN,
                occurredAt = 100L,
            )

        // 0 으로 접으면 "잠자리에 있었지만 한숨도 안 잤다"가 된다 — 서버가 durationMillis 로 대체한다.
        assertEquals(null, entity.toDomain().sleepMillis)
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
