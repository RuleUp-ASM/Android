package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.VerificationSignal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.1 sync 요청 (전송 스펙 §0.1 공통 envelope) ----------
// 모든 시각은 Long epoch millis 다(전송 스펙 설계 원칙 ①) — 문자열 변환을 거치지 않는다.

/**
 * 지오펜스 전이 1건(전송 스펙 §1). 좌표는 계약에 없어 보내지 않는다 — 좌표가 나가는 유일한 통로는
 * [LocationPointRequest] 다.
 *
 * 정확도·mock 여부는 OS 가 위치를 안 준 전이에서 null 이고, `explicitNulls=false` 라 그 경우
 * 필드가 통째로 빠진 채 전송된다.
 */
@Serializable
data class GeofenceEventRequest(
    // 등록 시 부여한 지오펜스 requestId
    @SerialName("anchorId")
    val anchorId: String,
    @SerialName("transition")
    val transition: String,
    @SerialName("observedAt")
    val observedAt: Long,
    @SerialName("observedElapsedMillis")
    val observedElapsedMillis: Long,
    @SerialName("accuracy")
    val accuracy: Double? = null,
    @SerialName("isMock")
    val isMock: Boolean? = null,
)

/** 대상 앱 전후면 전이 1건(전송 스펙 §3). 페어링·합산은 서버가 한다. */
@Serializable
data class AppEventRequest(
    @SerialName("packageName")
    val packageName: String,
    @SerialName("eventType")
    val eventType: String,
    @SerialName("at")
    val at: Long,
)

/** 보조 측위 샘플 1건(전송 스펙 §1). 3순위 좌표 가중 체류 챌린지에만 붙는다. */
@Serializable
data class LocationPointRequest(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
    @SerialName("accuracy")
    val accuracy: Double,
    @SerialName("isMock")
    val isMock: Boolean,
    @SerialName("at")
    val at: Long,
)

/**
 * Health Connect 읽은 값 1건(전송 스펙 §2). 집계·화이트리스트·MANUAL 거부는 서버가 한다.
 * 신뢰 메타데이터([recordingMethod]·[originPackage])와 멱등 키([recordId])는 필수 동봉이다 —
 * 값만 보내면 서버가 거부한다. `metric` 은 신호 레벨이라 여기서 반복하지 않는다.
 */
@Serializable
data class HealthReadingRequest(
    @SerialName("recordId")
    val recordId: String,
    @SerialName("value")
    val value: Double,
    @SerialName("startTime")
    val startTime: Long,
    @SerialName("endTime")
    val endTime: Long,
    @SerialName("recordingMethod")
    val recordingMethod: String,
    @SerialName("originPackage")
    val originPackage: String,
)

/** 수면 세션 1건(전송 스펙 §5). stage 를 쪼개지 않고 세션 단위로 보낸다. */
@Serializable
data class SleepSessionRequest(
    @SerialName("recordId")
    val recordId: String,
    @SerialName("start")
    val start: Long,
    @SerialName("end")
    val end: Long,
    @SerialName("durationMillis")
    val durationMillis: Long,
    // stage 를 못 받으면 생략된다 — 서버가 durationMillis 로 대체한다
    @SerialName("sleepMillis")
    val sleepMillis: Long? = null,
    @SerialName("observedElapsedMillis")
    val observedElapsedMillis: Long,
    @SerialName("recordingMethod")
    val recordingMethod: String,
    @SerialName("originPackage")
    val originPackage: String,
)

/**
 * 신호 1건 (전송 스펙 §1~§5). [type] 디스크리미네이터 + 타입별 옵셔널 필드.
 * GEOFENCE → [events], SCREEN_TIME → [appEvents], WAKE → [firstUnlock]/[firstScreenOn]/[deviceSecure],
 * LOCATION → [points], HEALTH → [date]/[metric]/[readings], SLEEP → [sessions].
 */
@Serializable
data class SignalRequest(
    @SerialName("type")
    val type: String,
    @SerialName("events")
    val events: List<GeofenceEventRequest>? = null,
    @SerialName("appEvents")
    val appEvents: List<AppEventRequest>? = null,
    @SerialName("firstUnlock")
    val firstUnlock: Long? = null,
    @SerialName("firstScreenOn")
    val firstScreenOn: Long? = null,
    @SerialName("deviceSecure")
    val deviceSecure: Boolean? = null,
    @SerialName("points")
    val points: List<LocationPointRequest>? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("metric")
    val metric: String? = null,
    @SerialName("readings")
    val readings: List<HealthReadingRequest>? = null,
    @SerialName("sessions")
    val sessions: List<SleepSessionRequest>? = null,
)

// ---------- §0.1 공통 envelope 필드 ----------

/** Health Connect 신호별 권한 현황(전송 스펙 §0.1 permissions.healthConnect). */
@Serializable
data class HealthConnectPermissionsRequest(
    @SerialName("distance")
    val distance: String,
    @SerialName("steps")
    val steps: String,
    @SerialName("sleep")
    val sleep: String,
    @SerialName("background")
    val background: String,
)

/** 신호별 권한 현황 스냅샷(전송 스펙 §0.1 permissions). 값은 GRANTED/DENIED. */
@Serializable
data class PermissionsRequest(
    @SerialName("location")
    val location: String,
    @SerialName("backgroundLocation")
    val backgroundLocation: String,
    @SerialName("activityRecognition")
    val activityRecognition: String,
    @SerialName("usageStats")
    val usageStats: String,
    @SerialName("postNotifications")
    val postNotifications: String,
    @SerialName("healthConnect")
    val healthConnect: HealthConnectPermissionsRequest,
)

/** VPN 게이트(전송 스펙 §6.1). */
@Serializable
data class NetworkRequest(
    @SerialName("vpnActive")
    val vpnActive: Boolean,
)

/** Play Integrity verdict 토큰(전송 스펙 §6.5). token 없으면 envelope 에서 통째로 생략. */
@Serializable
data class IntegrityRequest(
    @SerialName("token")
    val token: String,
)

/** worker heartbeat 진단(전송 스펙 §0.7). null 필드는 explicitNulls=false 로 생략. */
@Serializable
data class DiagnosticsRequest(
    @SerialName("lastSuccessfulFlushAt")
    val lastSuccessfulFlushAt: Long? = null,
    @SerialName("standbyBucket")
    val standbyBucket: Int? = null,
    @SerialName("backgroundRestricted")
    val backgroundRestricted: Boolean? = null,
    @SerialName("isIgnoringBatteryOptimizations")
    val isIgnoringBatteryOptimizations: Boolean? = null,
    @SerialName("expeditedDeferred")
    val expeditedDeferred: Boolean? = null,
    @SerialName("lastGeofenceReregisterAt")
    val lastGeofenceReregisterAt: Long? = null,
    @SerialName("hcSdkStatus")
    val hcSdkStatus: String? = null,
)

/** 신호 공백 1건(전송 스펙 §0.5 gaps[]). 시각은 epoch millis. */
@Serializable
data class GapRequest(
    @SerialName("signalType")
    val signalType: String,
    @SerialName("reason")
    val reason: String,
    @SerialName("fromMillis")
    val fromMillis: Long,
    @SerialName("toMillis")
    val toMillis: Long,
    @SerialName("recoverable")
    val recoverable: Boolean,
)

/**
 * sync 1회 전송 단위 envelope(전송 스펙 §0.1). 신호 배치 + 디바이스 시계·권한·VPN·integrity·진단·gap.
 * 정적 프로필(sdkInt/model/lowRam/appVersion)은 Phase 0(로그인)에서만 보내고 여기엔 싣지 않는다.
 */
@Serializable
data class SyncEnvelopeRequest(
    @SerialName("deviceTimeMillis")
    val deviceTimeMillis: Long,
    @SerialName("elapsedRealtimeMillis")
    val elapsedRealtimeMillis: Long,
    @SerialName("bootSessionId")
    val bootSessionId: String,
    @SerialName("timeZone")
    val timeZone: String,
    @SerialName("activeChallengeIds")
    val activeChallengeIds: List<String>,
    @SerialName("permissions")
    val permissions: PermissionsRequest,
    @SerialName("network")
    val network: NetworkRequest,
    @SerialName("integrity")
    val integrity: IntegrityRequest? = null,
    @SerialName("diagnostics")
    val diagnostics: DiagnosticsRequest? = null,
    @SerialName("gaps")
    val gaps: List<GapRequest>,
    @SerialName("signals")
    val signals: List<SignalRequest>,
)

private fun VerificationSignal.toDto(): SignalRequest =
    when (this) {
        is VerificationSignal.GeofenceTransitions ->
            SignalRequest(
                type = "GEOFENCE",
                events =
                    events.map {
                        GeofenceEventRequest(
                            anchorId = it.requestId,
                            transition = it.transition.name,
                            observedAt = it.observedAt,
                            observedElapsedMillis = it.observedElapsedMillis,
                            accuracy = it.accuracy?.toDouble(),
                            isMock = it.isMock,
                        )
                    },
            )

        is VerificationSignal.ScreenTime ->
            SignalRequest(
                type = "SCREEN_TIME",
                appEvents =
                    appEvents.map {
                        AppEventRequest(
                            packageName = it.packageName,
                            eventType = it.eventType.name,
                            at = it.at,
                        )
                    },
            )

        is VerificationSignal.Wake ->
            SignalRequest(
                type = "WAKE",
                firstUnlock = firstUnlock,
                firstScreenOn = firstScreenOn,
                deviceSecure = deviceSecure,
            )

        is VerificationSignal.Locations ->
            SignalRequest(
                type = "LOCATION",
                points =
                    points.map {
                        LocationPointRequest(
                            lat = it.lat,
                            lng = it.lng,
                            accuracy = it.accuracy.toDouble(),
                            isMock = it.isMock,
                            at = it.at,
                        )
                    },
            )

        is VerificationSignal.Health ->
            SignalRequest(
                type = "HEALTH",
                date = date,
                metric = metric.name,
                readings =
                    readings.map {
                        HealthReadingRequest(
                            recordId = it.recordId,
                            value = it.value,
                            startTime = it.startTime,
                            endTime = it.endTime,
                            recordingMethod = it.recordingMethod.name,
                            originPackage = it.originPackage,
                        )
                    },
            )

        is VerificationSignal.Sleep ->
            SignalRequest(
                type = "SLEEP",
                sessions =
                    sessions.map {
                        SleepSessionRequest(
                            recordId = it.recordId,
                            start = it.start,
                            end = it.end,
                            durationMillis = it.durationMillis,
                            sleepMillis = it.sleepMillis,
                            observedElapsedMillis = it.observedElapsedMillis,
                            recordingMethod = it.recordingMethod.name,
                            originPackage = it.originPackage,
                        )
                    },
            )
    }

internal fun PermissionSnapshot.toDto(): PermissionsRequest =
    PermissionsRequest(
        location = location.name,
        backgroundLocation = backgroundLocation.name,
        activityRecognition = activityRecognition.name,
        usageStats = usageStats.name,
        postNotifications = postNotifications.name,
        healthConnect =
            HealthConnectPermissionsRequest(
                distance = healthDistance.name,
                steps = healthSteps.name,
                sleep = healthSleep.name,
                background = healthBackground.name,
            ),
    )

private fun SignalGap.toDto(): GapRequest =
    GapRequest(
        signalType = signalType,
        reason = reason.name,
        fromMillis = fromMillis,
        toMillis = toMillis,
        recoverable = recoverable,
    )

/**
 * 도메인 envelope 메타데이터 + 신호 배치 → §0.1 envelope 와이어. 정적 프로필은 제외(Phase 0 전용).
 */
internal fun EnvelopeMetadata.toRequest(batch: SignalBatch): SyncEnvelopeRequest =
    SyncEnvelopeRequest(
        deviceTimeMillis = clock.deviceTimeMillis,
        elapsedRealtimeMillis = clock.elapsedRealtimeMillis,
        bootSessionId = clock.bootSessionId,
        timeZone = clock.timeZone,
        activeChallengeIds = activeChallengeIds,
        permissions = permissions.toDto(),
        network = NetworkRequest(vpnActive = network.vpnActive),
        integrity = integrity.token?.let { IntegrityRequest(token = it) },
        diagnostics =
            DiagnosticsRequest(
                lastSuccessfulFlushAt = diagnostics.lastSuccessfulFlushAt,
                standbyBucket = diagnostics.standbyBucket,
                backgroundRestricted = diagnostics.backgroundRestricted,
                isIgnoringBatteryOptimizations = diagnostics.isIgnoringBatteryOptimizations,
                expeditedDeferred = diagnostics.expeditedDeferred,
                lastGeofenceReregisterAt = diagnostics.lastGeofenceReregisterAt,
                hcSdkStatus = diagnostics.hcSdkStatus,
            ),
        gaps = gaps.map { it.toDto() },
        signals = batch.signals.map { it.toDto() },
    )
