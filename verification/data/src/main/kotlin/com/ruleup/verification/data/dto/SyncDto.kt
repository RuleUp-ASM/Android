package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.UpdatedChallenge
import com.ruleup.verification.domain.entity.VerificationSignal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ---------- 3.1 sync 요청 (명세 §3.2 스코핑 페이로드) ----------

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

@Serializable
data class SyncRequest(
    @SerialName("collectedAt")
    val collectedAt: String,
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

internal fun SignalBatch.toRequest(): SyncRequest =
    SyncRequest(
        collectedAt = collectedAt,
        signals = signals.map { it.toDto() },
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
