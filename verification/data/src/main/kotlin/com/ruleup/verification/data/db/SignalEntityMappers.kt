package com.ruleup.verification.data.db

import com.ruleup.verification.domain.entity.AppEventType
import com.ruleup.verification.domain.entity.AppUsageEvent
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.GeofenceTransitionEvent
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import com.ruleup.verification.domain.entity.HealthDeviceType
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthOrigin
import com.ruleup.verification.domain.entity.HealthReading
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.entity.LocationPoint
import com.ruleup.verification.domain.entity.RecordingMethod
import com.ruleup.verification.domain.entity.ScreenEvent
import com.ruleup.verification.domain.entity.ScreenEventType
import com.ruleup.verification.domain.entity.SleepSegment

internal fun GeofenceTransitionEntity.toDomain(): GeofenceTransitionEvent =
    GeofenceTransitionEvent(
        requestId = requestId,
        transition = transition.toGeofenceTransitionType(),
        at = occurredAt,
        lat = lat,
        lng = lng,
        accuracy = accuracy,
        isMock = isMock,
    )

internal fun LocationSampleEntity.toDomain(): LocationPoint =
    LocationPoint(
        lat = lat,
        lng = lng,
        accuracy = accuracy,
        isMock = isMock,
        at = occurredAt,
    )

internal fun GeofenceTargetEntity.toDomain(): GeofenceTarget =
    GeofenceTarget(
        requestId = requestId,
        lat = lat,
        lng = lng,
        radiusM = radiusM,
        dwellMinutes = dwellMinutes,
    )

internal fun GeofenceTarget.toEntity(): GeofenceTargetEntity =
    GeofenceTargetEntity(
        requestId = requestId,
        lat = lat,
        lng = lng,
        radiusM = radiusM,
        dwellMinutes = dwellMinutes,
    )

/** kind=APP 인 행만 앱 사용 이벤트로 변환(아니면 null). */
internal fun UsageEventEntity.toAppEvent(): AppUsageEvent? =
    if (kind != KIND_APP) {
        null
    } else {
        AppUsageEvent(
            packageName = packageName,
            eventType = eventType.toAppEventType(),
            at = occurredAt,
        )
    }

/** kind=SCREEN 인 행만 화면/잠금해제 이벤트로 변환(아니면 null). */
internal fun UsageEventEntity.toScreenEvent(): ScreenEvent? =
    if (kind != KIND_SCREEN) {
        null
    } else {
        ScreenEvent(
            event = eventType.toScreenEventType(),
            at = occurredAt,
        )
    }

internal fun HealthReadingEntity.toDomain(): HealthReading =
    HealthReading(
        metric = metric.toHealthMetric(),
        value = value,
        unit = unit,
        exerciseType = exerciseType,
        origin =
            HealthOrigin(
                dataOrigin = dataOrigin,
                recordingMethod = recordingMethod.toRecordingMethod(),
                deviceType = deviceType.toHealthDeviceType(),
            ),
        at = occurredAt,
    )

internal fun SleepSegmentEntity.toDomain(): SleepSegment =
    SleepSegment(
        startAt = startAt,
        endAt = endAt,
        status = status,
    )

internal fun HealthTargetEntity.toDomain(): HealthTarget =
    HealthTarget(
        metric = metric.toHealthMetric(),
        exerciseType = exerciseType,
    )

internal fun HealthTarget.toEntity(): HealthTargetEntity =
    HealthTargetEntity(
        metric = metric.name,
        exerciseType = exerciseType,
    )

// 저장값은 항상 enum.name 이므로 안전하나, 미인식은 보수적으로 ENTER 로 떨군다.
private fun String.toGeofenceTransitionType(): GeofenceTransitionType =
    GeofenceTransitionType.entries.find { it.name == this } ?: GeofenceTransitionType.ENTER

// 저장값은 enum.name. 미인식 metric 은 보수적으로 STEPS 로(가장 흔한 무해 기본).
private fun String.toHealthMetric(): HealthMetric = HealthMetric.entries.find { it.name == this } ?: HealthMetric.STEPS

private fun String.toRecordingMethod(): RecordingMethod = RecordingMethod.entries.find { it.name == this } ?: RecordingMethod.UNKNOWN

private fun String.toHealthDeviceType(): HealthDeviceType = HealthDeviceType.entries.find { it.name == this } ?: HealthDeviceType.UNKNOWN

private fun String.toAppEventType(): AppEventType = AppEventType.entries.find { it.name == this } ?: AppEventType.RESUMED

private fun String.toScreenEventType(): ScreenEventType = ScreenEventType.entries.find { it.name == this } ?: ScreenEventType.UNLOCK
