package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.UpdatedChallenge
import com.ruleup.verification.domain.entity.VerificationSignal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ---------- 3.1 sync 요청 (전송 스펙 §0.1 공통 envelope) ----------

/** epoch millis → ISO-8601 (BE 가 KST 로 변환해 target_date 산정, 명세 §2.10). */
@OptIn(ExperimentalTime::class)
private fun Long.toIso(): String = Instant.fromEpochMilliseconds(this).toString()

@Serializable
data class GeofenceEventDto(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("transition")
    val transition: String,
    @SerialName("at")
    val at: String,
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
    @SerialName("accuracy")
    val accuracy: Double,
    @SerialName("isMock")
    val isMock: Boolean,
)

@Serializable
data class AppEventDto(
    @SerialName("packageName")
    val packageName: String,
    @SerialName("eventType")
    val eventType: String,
    @SerialName("at")
    val at: String,
)

@Serializable
data class ScreenEventDto(
    @SerialName("event")
    val event: String,
    @SerialName("at")
    val at: String,
)

@Serializable
data class LocationPointDto(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
    @SerialName("accuracy")
    val accuracy: Double,
    @SerialName("isMock")
    val isMock: Boolean,
    @SerialName("at")
    val at: String,
)

/** HEALTH readings 의 신뢰 메타데이터(명세 §6.2·§8.2). 필수 동봉 — 없으면 BE 거부. */
@Serializable
data class HealthOriginDto(
    @SerialName("dataOrigin")
    val dataOrigin: String,
    @SerialName("recordingMethod")
    val recordingMethod: String,
    @SerialName("deviceType")
    val deviceType: String,
)

/** Health Connect 읽은 값 1건(명세 §6.2). 집계·게이트는 BE. */
@Serializable
data class HealthReadingDto(
    @SerialName("metric")
    val metric: String,
    @SerialName("value")
    val value: Double,
    @SerialName("unit")
    val unit: String,
    @SerialName("exerciseType")
    val exerciseType: String? = null,
    @SerialName("origin")
    val origin: HealthOriginDto,
)

/** 수면 세그먼트 1건(명세 §6.2). */
@Serializable
data class SleepSegmentDto(
    @SerialName("startAt")
    val startAt: String,
    @SerialName("endAt")
    val endAt: String,
    @SerialName("status")
    val status: String,
)

/**
 * 신호 1건 (명세 §3.2). [type] 디스크리미네이터 + 타입별 옵셔널 필드.
 * GEOFENCE_TRANSITION → [events], SCREEN_TIME → [appEvents]/[screenEvents], LOCATION → [points],
 * HEALTH → [date]/[readings], SLEEP → [segments].
 */
@Serializable
data class SignalDto(
    @SerialName("type")
    val type: String,
    @SerialName("events")
    val events: List<GeofenceEventDto>? = null,
    @SerialName("appEvents")
    val appEvents: List<AppEventDto>? = null,
    @SerialName("screenEvents")
    val screenEvents: List<ScreenEventDto>? = null,
    @SerialName("points")
    val points: List<LocationPointDto>? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("readings")
    val readings: List<HealthReadingDto>? = null,
    @SerialName("segments")
    val segments: List<SleepSegmentDto>? = null,
)

// ---------- §0.1 공통 envelope 필드 ----------

/** Health Connect 신호별 권한 현황(전송 스펙 §0.1 permissions.healthConnect). */
@Serializable
data class HealthConnectPermissionsDto(
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
data class PermissionsDto(
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
    val healthConnect: HealthConnectPermissionsDto,
)

/** VPN 게이트(전송 스펙 §6.1). */
@Serializable
data class NetworkDto(
    @SerialName("vpnActive")
    val vpnActive: Boolean,
)

/** Play Integrity verdict 토큰(전송 스펙 §6.5). token 없으면 envelope 에서 통째로 생략. */
@Serializable
data class IntegrityDto(
    @SerialName("token")
    val token: String,
)

/** worker heartbeat 진단(전송 스펙 §0.7). null 필드는 explicitNulls=false 로 생략. */
@Serializable
data class DiagnosticsDto(
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
data class GapDto(
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
    val permissions: PermissionsDto,
    @SerialName("network")
    val network: NetworkDto,
    @SerialName("integrity")
    val integrity: IntegrityDto? = null,
    @SerialName("diagnostics")
    val diagnostics: DiagnosticsDto? = null,
    @SerialName("gaps")
    val gaps: List<GapDto>,
    @SerialName("signals")
    val signals: List<SignalDto>,
)

private fun VerificationSignal.toDto(): SignalDto =
    when (this) {
        is VerificationSignal.GeofenceTransitions ->
            SignalDto(
                type = "GEOFENCE_TRANSITION",
                events =
                    events.map {
                        GeofenceEventDto(
                            requestId = it.requestId,
                            transition = it.transition.name,
                            at = it.at.toIso(),
                            lat = it.lat,
                            lng = it.lng,
                            accuracy = it.accuracy.toDouble(),
                            isMock = it.isMock,
                        )
                    },
            )

        is VerificationSignal.ScreenTime ->
            SignalDto(
                type = "SCREEN_TIME",
                appEvents =
                    appEvents.map {
                        AppEventDto(
                            packageName = it.packageName,
                            eventType = it.eventType.name,
                            at = it.at.toIso(),
                        )
                    },
                screenEvents =
                    screenEvents.map {
                        ScreenEventDto(
                            event = it.event.name,
                            at = it.at.toIso(),
                        )
                    },
            )

        is VerificationSignal.Locations ->
            SignalDto(
                type = "LOCATION",
                points =
                    points.map {
                        LocationPointDto(
                            lat = it.lat,
                            lng = it.lng,
                            accuracy = it.accuracy.toDouble(),
                            isMock = it.isMock,
                            at = it.at.toIso(),
                        )
                    },
            )

        is VerificationSignal.Health ->
            SignalDto(
                type = "HEALTH",
                date = date,
                readings =
                    readings.map {
                        HealthReadingDto(
                            metric = it.metric.name,
                            value = it.value,
                            unit = it.unit,
                            exerciseType = it.exerciseType,
                            origin =
                                HealthOriginDto(
                                    dataOrigin = it.origin.dataOrigin,
                                    recordingMethod = it.origin.recordingMethod.name,
                                    deviceType = it.origin.deviceType.name,
                                ),
                        )
                    },
            )

        is VerificationSignal.Sleep ->
            SignalDto(
                type = "SLEEP",
                segments =
                    segments.map {
                        SleepSegmentDto(
                            startAt = it.startAt.toIso(),
                            endAt = it.endAt.toIso(),
                            status = it.status,
                        )
                    },
            )
    }

internal fun PermissionSnapshot.toDto(): PermissionsDto =
    PermissionsDto(
        location = location.name,
        backgroundLocation = backgroundLocation.name,
        activityRecognition = activityRecognition.name,
        usageStats = usageStats.name,
        postNotifications = postNotifications.name,
        healthConnect =
            HealthConnectPermissionsDto(
                distance = healthDistance.name,
                steps = healthSteps.name,
                sleep = healthSleep.name,
                background = healthBackground.name,
            ),
    )

private fun SignalGap.toDto(): GapDto =
    GapDto(
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
        network = NetworkDto(vpnActive = network.vpnActive),
        integrity = integrity.token?.let { IntegrityDto(token = it) },
        diagnostics =
            DiagnosticsDto(
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

// ---------- 3.1 sync 응답 ----------
@Serializable
data class UpdatedChallengeDto(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("todayStatus")
    val todayStatus: String? = null,
    @SerialName("progressRate")
    val progressRate: Double? = null,
)

@Serializable
data class SyncResponse(
    @SerialName("syncedAt")
    val syncedAt: String? = null,
    @SerialName("nextSyncAfterSec")
    val nextSyncAfterSec: Int? = null,
    @SerialName("updatedChallenges")
    val updatedChallenges: List<UpdatedChallengeDto>? = null,
    @SerialName("ignoredSignalTypes")
    val ignoredSignalTypes: List<String>? = null,
)

internal fun SyncResponse.toDomain(): SyncResult =
    SyncResult(
        syncedAt = syncedAt.orEmpty(),
        // 명세 응답 예시 기본 1800초(30분).
        nextSyncAfterSec = nextSyncAfterSec ?: DEFAULT_NEXT_SYNC_SEC,
        updatedChallenges =
            updatedChallenges.orEmpty().mapNotNull { dto ->
                val id = dto.challengeId ?: return@mapNotNull null
                UpdatedChallenge(
                    challengeId = id,
                    todayStatus = TodayStatus.fromValue(dto.todayStatus),
                    progressRate = dto.progressRate ?: 0.0,
                )
            },
        ignoredSignalTypes = ignoredSignalTypes.orEmpty(),
    )

private const val DEFAULT_NEXT_SYNC_SEC = 1800
