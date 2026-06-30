package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.DeviceClock
import com.ruleup.verification.domain.entity.DeviceDiagnostics
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.GeofenceTransitionEvent
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import com.ruleup.verification.domain.entity.HealthDeviceType
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthOrigin
import com.ruleup.verification.domain.entity.HealthReading
import com.ruleup.verification.domain.entity.IntegritySnapshot
import com.ruleup.verification.domain.entity.NetworkState
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.RecordingMethod
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SleepSegment
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.VerificationSignal
import com.ruleup.verification.domain.entity.VerifiedVia
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
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    /** 테스트용 envelope 메타데이터(§0.1). 신호 배치와 합쳐 envelope 와이어로 직렬화한다. */
    private fun metadata(gaps: List<SignalGap> = emptyList()): EnvelopeMetadata =
        EnvelopeMetadata(
            clock =
                DeviceClock(
                    deviceTimeMillis = 1_719_600_000_000L,
                    elapsedRealtimeMillis = 987_654_321L,
                    bootSessionId = "boot-1",
                    timeZone = "Asia/Seoul",
                ),
            activeChallengeIds = listOf("c-1"),
            permissions =
                PermissionSnapshot(
                    location = PermissionState.GRANTED,
                    backgroundLocation = PermissionState.DENIED,
                    activityRecognition = PermissionState.GRANTED,
                    usageStats = PermissionState.GRANTED,
                    postNotifications = PermissionState.GRANTED,
                    healthDistance = PermissionState.GRANTED,
                    healthSteps = PermissionState.DENIED,
                    healthSleep = PermissionState.GRANTED,
                    healthBackground = PermissionState.GRANTED,
                ),
            network = NetworkState(vpnActive = false),
            integrity = IntegritySnapshot(token = null),
            diagnostics = DeviceDiagnostics(null, null, null, null, null, null, "SDK_AVAILABLE"),
            gaps = gaps,
        )

    @Test
    fun `envelope 는 디바이스 시계·권한·gap·신호를 함께 직렬화하고 token 없으면 integrity 를 생략한다`() {
        val batch = SignalBatch(collectedAt = "2026-06-21T00:00:00Z", signals = emptyList())
        val gap = SignalGap("HEALTH", GapReason.PERMISSION_MISSING, fromMillis = 10L, toMillis = 20L, recoverable = true)

        val encoded = json.encodeToString(metadata(gaps = listOf(gap)).toRequest(batch))
        val decoded = json.decodeFromString<SyncEnvelopeRequest>(encoded)

        assertEquals(1_719_600_000_000L, decoded.deviceTimeMillis)
        assertEquals("boot-1", decoded.bootSessionId)
        assertEquals("Asia/Seoul", decoded.timeZone)
        assertEquals(listOf("c-1"), decoded.activeChallengeIds)
        assertEquals("GRANTED", decoded.permissions.location)
        assertEquals("DENIED", decoded.permissions.backgroundLocation)
        assertEquals("DENIED", decoded.permissions.healthConnect.steps)
        assertEquals(false, decoded.network.vpnActive)
        // token 없으면 integrity 객체 통째로 생략(explicitNulls=false).
        assertTrue(!encoded.contains("\"integrity\""))
        val g = decoded.gaps.single()
        assertEquals("HEALTH", g.signalType)
        assertEquals("PERMISSION_MISSING", g.reason)
        assertEquals(true, g.recoverable)
    }

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

        val request = metadata().toRequest(batch)
        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<SyncEnvelopeRequest>(encoded)

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
    fun `HEALTH 신호가 date·readings·origin 메타데이터와 함께 라운드트립한다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-24T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.Health(
                            date = "2026-06-24",
                            readings =
                                listOf(
                                    HealthReading(
                                        metric = HealthMetric.DISTANCE,
                                        value = 5.2,
                                        unit = "km",
                                        exerciseType = "RUNNING",
                                        origin =
                                            HealthOrigin(
                                                dataOrigin = "com.sec.android.app.shealth",
                                                recordingMethod = RecordingMethod.AUTO,
                                                deviceType = HealthDeviceType.WATCH,
                                            ),
                                        // at 은 내부 정렬용 — 페이로드엔 미포함.
                                        at = 1_700_000_000_000L,
                                    ),
                                ),
                        ),
                    ),
            )

        val decoded = json.decodeFromString<SyncEnvelopeRequest>(json.encodeToString(metadata().toRequest(batch)))
        val signal = decoded.signals.single()
        assertEquals("HEALTH", signal.type)
        assertEquals("2026-06-24", signal.date)
        val reading = assertNotNull(signal.readings).single()
        assertEquals("DISTANCE", reading.metric)
        assertEquals(5.2, reading.value)
        assertEquals("km", reading.unit)
        assertEquals("RUNNING", reading.exerciseType)
        // origin(신뢰 게이트 입력)은 필수 동봉(명세 §6.2·§8.2).
        assertEquals("com.sec.android.app.shealth", reading.origin.dataOrigin)
        assertEquals("AUTO", reading.origin.recordingMethod)
        assertEquals("WATCH", reading.origin.deviceType)
    }

    @Test
    fun `SLEEP 신호 세그먼트가 epoch 에서 ISO 로 라운드트립한다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-24T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.Sleep(
                            segments = listOf(SleepSegment(startAt = 0L, endAt = 3_600_000L, status = "DEEP")),
                        ),
                    ),
            )

        val decoded = json.decodeFromString<SyncEnvelopeRequest>(json.encodeToString(metadata().toRequest(batch)))
        val signal = decoded.signals.single()
        assertEquals("SLEEP", signal.type)
        val segment = assertNotNull(signal.segments).single()
        assertEquals("1970-01-01T00:00:00Z", segment.startAt)
        assertEquals("1970-01-01T01:00:00Z", segment.endAt)
        assertEquals("DEEP", segment.status)
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
    fun `카카오 장소 검색 응답은 좌표 없는 항목을 거르고 매핑된다`() {
        val payload =
            """
            { "documents": [
                { "place_name": "스포애니 강남", "x": "127.0", "y": "37.5",
                  "road_address_name": "서울 강남구 테헤란로", "address_name": "서울 강남구",
                  "category_group_name": "헬스장", "category_name": "스포츠,레저 > 헬스장" },
                { "place_name": "좌표없음", "address_name": "주소만 있음" }
            ] }
            """.trimIndent()

        val places = json.decodeFromString<KakaoKeywordResponse>(payload).toDomain()

        // 좌표(x=경도, y=위도) 없는 항목은 앵커로 못 쓰므로 제외(명세 §5.2).
        assertEquals(1, places.size)
        assertEquals("스포애니 강남", places.single().name)
        assertEquals(37.5, places.single().lat)
        assertEquals(127.0, places.single().lng)
        // 도로명 주소·카테고리 그룹명 우선.
        assertEquals("서울 강남구 테헤란로", places.single().address)
        assertEquals("헬스장", places.single().category)
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
        assertEquals(
            false,
            detail.methods
                .single()
                .detail
                ?.insideGeofence,
        )
        assertEquals(TodayStatus.SUCCESS, detail.dailyLogs.single().status)
    }

    @Test
    fun `검증 상세 today 의 verifiedVia·disputeClosesAt 과 HEALTH 근거가 매핑된다`() {
        val payload =
            """
            {
              "challengeId": "c-1", "title": "러닝 3km", "status": "ACTIVE",
              "verification": {
                "overallStatus": "ON_TRACK", "progressRate": 50.0,
                "today": {
                  "isTarget": true, "status": "SUCCESS",
                  "verifiedVia": "MANUAL_FALLBACK", "disputeClosesAt": "2026-06-25T00:00:00Z",
                  "evidence": { "distanceKm": 5.2, "steps": 8000, "healthOrigin": "com.sec.android.app.shealth" }
                },
                "methods": [
                  { "method": "HEALTH",
                    "detail": { "metric": "DISTANCE", "value": 5.2, "goal": 3.0,
                                "dataOrigin": "com.sec.android.app.shealth", "recordingMethod": "AUTO" } }
                ]
              }
            }
            """.trimIndent()
        val detail = json.decodeFromString<VerificationDetailResponse>(payload).toDomain()

        // 예비 폴백 잠정 성공 상태(명세 §9.2).
        assertEquals(VerifiedVia.MANUAL_FALLBACK, detail.today.verifiedVia)
        assertEquals("2026-06-25T00:00:00Z", detail.today.disputeClosesAt)
        assertEquals(5.2, detail.today.evidence?.distanceKm)
        assertEquals(8000, detail.today.evidence?.steps)
        // HEALTH method detail(명세 §8 신뢰 메타데이터 포함).
        val health = detail.methods.single().detail
        assertEquals("DISTANCE", health?.metric)
        assertEquals(3.0, health?.goal)
        assertEquals("AUTO", health?.recordingMethod)
    }
}
