package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.GeofenceTransitionEvent
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.VerificationSignal
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 0 수용 기준: DTO 직렬화/매핑 라운드트립.
 * (서버 응답의 미인식/누락 필드가 안전한 기본값으로 떨어지는지도 함께 검증)
 */
class VerificationDtoSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SignalBatch 를 sync 요청으로 매핑하면 epoch 가 ISO 로 변환되고 라운드트립한다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-21T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.GeofenceTransitions(
                            events =
                                listOf(
                                    GeofenceTransitionEvent(
                                        requestId = "member-1",
                                        transition = GeofenceTransitionType.DWELL,
                                        at = 0L,
                                        lat = 37.49,
                                        lng = 127.02,
                                        accuracy = 12.5f,
                                        isMock = false,
                                    ),
                                ),
                        ),
                    ),
            )

        val request = batch.toRequest()
        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<SyncRequest>(encoded)

        assertEquals("2026-06-21T00:00:00Z", decoded.collectedAt)
        val signal = decoded.signals.single()
        assertEquals("GEOFENCE_TRANSITION", signal.type)
        val event = assertNotNull(signal.events).single()
        assertEquals("member-1", event.requestId)
        assertEquals("DWELL", event.transition)
        // epoch 0 → ISO. isMock 은 처음부터 전송(명세 §7).
        assertEquals("1970-01-01T00:00:00Z", event.at)
        assertEquals(false, event.isMock)
    }

    @Test
    fun `sync 응답은 nextSyncAfterSec 누락 시 기본 1800초로 떨어진다`() {
        val payload = """{ "syncedAt": "2026-06-21T00:30:00Z", "updatedChallenges": [] }"""
        val result = json.decodeFromString<SyncResponse>(payload).toDomain()

        assertEquals(1800, result.nextSyncAfterSec)
        assertTrue(result.updatedChallenges.isEmpty())
        assertTrue(result.ignoredSignalTypes.isEmpty())
    }

    @Test
    fun `진행률 응답의 미인식 todayStatus 는 PENDING 으로 떨어진다`() {
        val payload =
            """
            {
              "asOf": "2026-06-21T09:00:00Z",
              "challenges": [
                { "challengeId": "c-1", "title": "헬스장 가기", "progressRate": 42.5,
                  "todayTarget": true, "todayStatus": "WHATEVER_NEW_VALUE" }
              ]
            }
            """.trimIndent()
        val snapshot = json.decodeFromString<ProgressResponse>(payload).toDomain()

        val challenge = snapshot.challenges.single()
        assertEquals("c-1", challenge.challengeId)
        assertEquals(42.5, challenge.progressRate)
        // 미인식 값은 보수적으로 PENDING(진행 중) — 실패로 표시 금지(명세 §6.4).
        assertEquals(TodayStatus.PENDING, challenge.todayStatus)
    }

    @Test
    fun `검증 상세 응답의 failureReason 과 today 상태가 매핑된다`() {
        val payload =
            """
            {
              "challengeId": "c-1", "title": "헬스장 가기", "status": "ACTIVE",
              "verification": {
                "overallStatus": "AT_RISK", "progressRate": 50.0,
                "successDays": 5, "targetDays": 10, "remainingDays": 5,
                "today": { "isTarget": true, "status": "FAILED", "failureReason": "OUT_OF_GEOFENCE" },
                "methods": [ { "method": "GPS", "detail": { "insideGeofence": false, "dwellMinutes": 12 } } ],
                "dailyLogs": [ { "date": "2026-06-20", "status": "SUCCESS", "method": "GPS" } ]
              }
            }
            """.trimIndent()
        val detail = json.decodeFromString<VerificationDetailResponse>(payload).toDomain()

        assertEquals("c-1", detail.challengeId)
        assertEquals(TodayStatus.FAILED, detail.today.status)
        assertEquals(FailureReason.OUT_OF_GEOFENCE, detail.today.failureReason)
        // supported 누락 → Android 기본 true.
        assertTrue(detail.methods.single().supported)
        assertEquals(false, detail.methods.single().detail?.insideGeofence)
        assertEquals(TodayStatus.SUCCESS, detail.dailyLogs.single().status)
    }
}
