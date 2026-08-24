package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.DeviceClock
import com.ruleup.verification.domain.entity.DeviceDiagnostics
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.GeofenceTransitionEvent
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthReading
import com.ruleup.verification.domain.entity.IntegritySnapshot
import com.ruleup.verification.domain.entity.NetworkState
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.RecordingMethod
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SleepSession
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
    fun `SignalBatch 를 sync 요청으로 매핑하면 GEOFENCE 계약대로 라운드트립한다`() {
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
                                        observedAt = 1_719_600_000_000L,
                                        observedElapsedMillis = 987_654_321L,
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
        assertEquals("GEOFENCE", signal.type)
        val event = assertNotNull(signal.events).single()
        assertEquals("member-1", event.anchorId)
        assertEquals("DWELL", event.transition)
        // 시각은 전부 epoch millis 다(전송 스펙 설계 원칙 ①).
        assertEquals(1_719_600_000_000L, event.observedAt)
        assertEquals(987_654_321L, event.observedElapsedMillis)
        assertEquals(false, event.isMock)
        // 좌표는 계약에 없다 — 전이 이벤트로 위치가 새어 나가면 안 된다.
        assertTrue(!encoded.contains("\"lat\""))
        assertTrue(!encoded.contains("\"lng\""))
    }

    @Test
    fun `WAKE 는 시퀀스가 아니라 당일 첫 시각과 deviceSecure 로 나간다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-21T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.Wake(
                            firstUnlock = 1_719_600_000_000L,
                            firstScreenOn = 1_719_599_000_000L,
                            deviceSecure = true,
                        ),
                    ),
            )

        val encoded = json.encodeToString(metadata().toRequest(batch))
        val signal = json.decodeFromString<SyncEnvelopeRequest>(encoded).signals.single()

        assertEquals("WAKE", signal.type)
        assertEquals(1_719_600_000_000L, signal.firstUnlock)
        assertEquals(1_719_599_000_000L, signal.firstScreenOn)
        assertEquals(true, signal.deviceSecure)
        // 기상 판정은 "첫 잠금해제가 목표 시각 안이었나" 하나만 묻는다 — raw 시퀀스를 실어 보내지 않는다.
        assertTrue(!encoded.contains("screenEvents"))
    }

    @Test
    fun `잠금해제가 없으면 WAKE 는 화면 켜짐 폴백만 담는다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-21T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.Wake(
                            firstUnlock = null,
                            firstScreenOn = 1_719_599_000_000L,
                            deviceSecure = false,
                        ),
                    ),
            )

        val encoded = json.encodeToString(metadata().toRequest(batch))
        val signal = json.decodeFromString<SyncEnvelopeRequest>(encoded).signals.single()

        // deviceSecure=false 는 잠금을 안 건 기기라는 뜻 — 서버가 폴백을 쓸지 가르는 입력이라 함께 간다.
        assertEquals(null, signal.firstUnlock)
        assertEquals(false, signal.deviceSecure)
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
                            metric = HealthMetric.DISTANCE,
                            readings =
                                listOf(
                                    HealthReading(
                                        recordId = "hc-1",
                                        value = 5.2,
                                        startTime = 1_699_999_000_000L,
                                        endTime = 1_700_000_000_000L,
                                        recordingMethod = RecordingMethod.AUTO,
                                        originPackage = "com.sec.android.app.shealth",
                                    ),
                                ),
                        ),
                    ),
            )

        val encoded = json.encodeToString(metadata().toRequest(batch))
        val signal = json.decodeFromString<SyncEnvelopeRequest>(encoded).signals.single()
        assertEquals("HEALTH", signal.type)
        assertEquals("2026-06-24", signal.date)
        // metric 은 신호 레벨이라 reading 마다 반복하지 않는다.
        assertEquals("DISTANCE", signal.metric)
        val reading = assertNotNull(signal.readings).single()
        assertEquals("hc-1", reading.recordId)
        assertEquals(5.2, reading.value)
        assertEquals(1_699_999_000_000L, reading.startTime)
        // 신뢰 게이트 입력은 필수 동봉 — 값만 보내면 서버가 거부한다.
        assertEquals("AUTO", reading.recordingMethod)
        assertEquals("com.sec.android.app.shealth", reading.originPackage)
        // 보내지 않기로 한 값들이 새어 나가지 않는다.
        assertTrue(!encoded.contains("\"unit\""))
        assertTrue(!encoded.contains("deviceType"))
    }

    @Test
    fun `SLEEP 은 stage 를 쪼개지 않고 세션 단위로 나간다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-24T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.Sleep(
                            sessions =
                                listOf(
                                    SleepSession(
                                        recordId = "sleep-1",
                                        start = 0L,
                                        end = 3_600_000L,
                                        durationMillis = 3_600_000L,
                                        sleepMillis = 3_000_000L,
                                        observedElapsedMillis = 987_654_321L,
                                        recordingMethod = RecordingMethod.AUTO,
                                        originPackage = "com.sec.android.app.shealth",
                                    ),
                                ),
                        ),
                    ),
            )

        val encoded = json.encodeToString(metadata().toRequest(batch))
        val signal = json.decodeFromString<SyncEnvelopeRequest>(encoded).signals.single()
        assertEquals("SLEEP", signal.type)
        val session = assertNotNull(signal.sessions).single()
        assertEquals("sleep-1", session.recordId)
        assertEquals(3_600_000L, session.durationMillis)
        assertEquals(3_000_000L, session.sleepMillis)
        assertEquals("com.sec.android.app.shealth", session.originPackage)
    }

    @Test
    fun `stage 를 못 받은 세션은 sleepMillis 없이 나간다`() {
        val batch =
            SignalBatch(
                collectedAt = "2026-06-24T00:00:00Z",
                signals =
                    listOf(
                        VerificationSignal.Sleep(
                            sessions =
                                listOf(
                                    SleepSession(
                                        recordId = "sleep-2",
                                        start = 0L,
                                        end = 3_600_000L,
                                        durationMillis = 3_600_000L,
                                        sleepMillis = null,
                                        observedElapsedMillis = 1L,
                                        recordingMethod = RecordingMethod.UNKNOWN,
                                        originPackage = "com.unknown.app",
                                    ),
                                ),
                        ),
                    ),
            )

        val encoded = json.encodeToString(metadata().toRequest(batch))
        // 0 으로 접으면 "한숨도 안 잤다"가 된다 — 필드를 통째로 빼서 서버가 durationMillis 로 대체하게 둔다.
        assertTrue(!encoded.contains("sleepMillis"))
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
