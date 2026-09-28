package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.VerificationSignal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 3.1 sync 요청 ---------- 모든 시각은 Long epoch millis 다

/** 지오펜스 전이 1건. */
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

/** 대상 앱 전후면 전이 1건. */
@Serializable
data class AppEventRequest(
    @SerialName("packageName")
    val packageName: String,
    @SerialName("eventType")
    val eventType: String,
    @SerialName("at")
    val at: Long,
)

/** 보조 측위 샘플 1건. */
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

/** Health Connect 읽은 값 1건. */
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

/** 수면 세션 1건. */
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
    // stage 를 못 받으면 생략된다
    @SerialName("sleepMillis")
    val sleepMillis: Long? = null,
    @SerialName("observedElapsedMillis")
    val observedElapsedMillis: Long,
    @SerialName("recordingMethod")
    val recordingMethod: String,
    @SerialName("originPackage")
    val originPackage: String,
)

/** 신호 1건. */
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

// §0.1 공통 envelope 필드

/** Health Connect 신호별 권한 현황. */
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

/** 신호별 권한 현황 스냅샷. */
@Serializable
data class PermissionsRequest(
    @SerialName("location")
    val location: String,
    @SerialName("backgroundLocation")
    val backgroundLocation: String,
    @SerialName("usageStats")
    val usageStats: String,
    @SerialName("postNotifications")
    val postNotifications: String,
    @SerialName("healthConnect")
    val healthConnect: HealthConnectPermissionsRequest,
)

/** VPN 게이트. */
@Serializable
data class NetworkRequest(
    @SerialName("vpnActive")
    val vpnActive: Boolean,
)

/** Play Integrity verdict 토큰. */
@Serializable
data class IntegrityRequest(
    @SerialName("token")
    val token: String,
)

/** worker heartbeat 진단. */
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

/** 신호 공백 1건. */
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

/** sync 1회 전송 단위 envelope. */
@Serializable
data class SyncEnvelopeRequest(
    // 로그인에 쓴 기기와 같은 값이어야 한다
    @SerialName("deviceId")
    val deviceId: String,
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
    // 이 구간의 신호를 빠짐없이 담았다는 선언(epoch millis).
    @SerialName("coveredFrom")
    val coveredFrom: Long,
    @SerialName("coveredUntil")
    val coveredUntil: Long,
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

/** 도메인 envelope 메타데이터 + 신호 배치 → §0.1 envelope 와이어. */
internal fun EnvelopeMetadata.toRequest(batch: SignalBatch): SyncEnvelopeRequest =
    SyncEnvelopeRequest(
        deviceId = deviceId,
        deviceTimeMillis = clock.deviceTimeMillis,
        elapsedRealtimeMillis = clock.elapsedRealtimeMillis,
        bootSessionId = clock.bootSessionId,
        timeZone = clock.timeZone,
        activeChallengeIds = activeChallengeIds,
        coveredFrom = coverage.from,
        coveredUntil = coverage.until,
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
