package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.CoverageWindow
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
import com.ruleup.verification.domain.entity.PendingReason
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.RecordingMethod
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SleepSession
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.VerificationSignal
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DTO 직렬화·매핑 라운드트립. 서버 응답의 미인식·누락 필드가 안전한 값으로 떨어지는지까지 본다. */
class VerificationDtoSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    /** 테스트용 envelope 메타데이터(§0.1). 신호 배치와 합쳐 envelope 와이어로 직렬화한다. */
    private fun metadata(gaps: List<SignalGap> = emptyList()): EnvelopeMetadata =
        EnvelopeMetadata(
            deviceId = "d-1",
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
            coverage = CoverageWindow(from = 1_719_598_200_000L, until = 1_719_600_000_000L),
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
        // 빠지면 서버가 모든 sync 를 400 INVALID_SIGNAL_PAYLOAD 로 반려한다.
        assertEquals(1_719_598_200_000L, decoded.coveredFrom)
        assertEquals(1_719_600_000_000L, decoded.coveredUntil)
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
    fun `sync 응답의 주기와 상한을 읽는다`() {
        val payload =
            """
            { "syncedAt": "2026-06-21T00:30:00Z", "flushIntervalSec": 900,
              "updatedChallenges": [], "maxPayloadBytes": 1048576 }
            """.trimIndent()

        val result = json.decodeFromString<SyncResponse>(payload).toDomain()

        // 서버가 주기를 30분에서 바꿔 내려도 못 읽으면 클라가 계속 1800초로 돈다.
        assertEquals(900, result.flushIntervalSec)
        assertEquals(1_048_576, result.maxPayloadBytes)
    }

    @Test
    fun `주기가 없으면 1800초로 떨어지고 상한은 모르는 채로 둔다`() {
        val payload = """{ "syncedAt": "2026-06-21T00:30:00Z", "updatedChallenges": [] }"""

        val result = json.decodeFromString<SyncResponse>(payload).toDomain()

        assertEquals(1800, result.flushIntervalSec)
        // 상한에 임의의 기본값을 박으면 서버가 낮춰도 클라가 그 사실을 모른 채 계속 초과 전송한다.
        assertNull(result.maxPayloadBytes)
        assertTrue(result.updatedChallenges.isEmpty())
        assertTrue(result.ignoredSignalTypes.isEmpty())
    }

    @Test
    fun `동의가 빠져 저장되지 않은 신호 종류를 읽는다`() {
        // 이 값을 버리면 사용자는 인증이 왜 안 되는지 모른 채 실패만 쌓는다.
        val payload =
            """
            { "syncedAt": "2026-06-21T00:30:00Z", "updatedChallenges": [],
              "consentRequired": ["LOCATION_INFO", "HEALTH_INFO"] }
            """.trimIndent()

        val result = json.decodeFromString<SyncResponse>(payload).toDomain()

        assertEquals(listOf("LOCATION_INFO", "HEALTH_INFO"), result.consentRequired)
    }

    @Test
    fun `동의 요구가 없으면 빈 목록이다`() {
        val payload = """{ "syncedAt": "2026-06-21T00:30:00Z", "updatedChallenges": [] }"""

        val result = json.decodeFromString<SyncResponse>(payload).toDomain()

        assertTrue(result.consentRequired.isEmpty())
    }

    @Test
    fun `실패 예정인 오늘 결과는 사유·근거·이의 창을 함께 읽는다`() {
        // 이의 창이 실패 예정이라, 여기서 사유와 근거를 버리면 사용자가 신청할지 판단할 수 없다.
        val payload =
            """
            {"date":"2026-07-25","status":"FAIL_EXPECTED","pendingReason":null,
             "failureReason":"WOKE_UP_LATE","evidenceSummary":"첫 잠금 해제 07:24 / 목표 07:00 이전",
             "appeal":{"eligibleUntil":"2026-07-27T00:00:00+09:00","eligible":true}}
            """.trimIndent()

        val today = json.decodeFromString<TodayResultResponse>(payload).toDomain()

        assertEquals(TodayResultStatus.FAIL_EXPECTED, today.status)
        assertEquals(FailureReason.WOKE_UP_LATE, today.failureReason)
        assertEquals("첫 잠금 해제 07:24 / 목표 07:00 이전", today.evidenceSummary)
        assertEquals(true, today.appeal?.eligible)
        assertNull(today.pendingReason)
    }

    @Test
    fun `판정 불가 사유는 권한과 신호 없음을 가르고 모르는 값은 비운다`() {
        // 모르는 사유를 권한 문제로 접으면 멀쩡한 사용자를 권한 화면으로 보낸다.
        val permission = json.decodeFromString<TodayResultResponse>("""{"pendingReason":"PERMISSION_MISSING"}""").toDomain()
        val unknown = json.decodeFromString<TodayResultResponse>("""{"pendingReason":"WAITING_SIGNAL"}""").toDomain()

        assertEquals(PendingReason.PERMISSION_MISSING, permission.pendingReason)
        assertNull(unknown.pendingReason)
    }

    @Test
    fun `진행률 응답의 미인식 todayStatus 는 어떤 상태로도 접지 않는다`() {
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
        // 진행 중으로 접으면 서버가 상태를 늘렸을 뿐인데 완료한 날이 진행 중으로 보인다.
        assertNull(challenge.todayStatus)
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
}
