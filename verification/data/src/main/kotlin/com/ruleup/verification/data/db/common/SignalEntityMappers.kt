package com.ruleup.verification.data.db.common

import com.ruleup.verification.data.db.geofence.GeofenceTargetEntity
import com.ruleup.verification.data.db.geofence.GeofenceTransitionEntity
import com.ruleup.verification.data.db.geofence.LocationSampleEntity
import com.ruleup.verification.data.db.health.HealthReadingEntity
import com.ruleup.verification.data.db.health.HealthTargetEntity
import com.ruleup.verification.data.db.health.SleepSessionEntity
import com.ruleup.verification.data.db.usage.KIND_APP
import com.ruleup.verification.data.db.usage.UsageEventEntity
import com.ruleup.verification.domain.entity.AppEventType
import com.ruleup.verification.domain.entity.AppUsageEvent
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.GeofenceTransitionEvent
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthReading
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.entity.LocationPoint
import com.ruleup.verification.domain.entity.RecordingMethod
import com.ruleup.verification.domain.entity.SleepSession

internal fun GeofenceTransitionEntity.toDomain(): GeofenceTransitionEvent =
    GeofenceTransitionEvent(
        requestId = requestId,
        transition = transition.toGeofenceTransitionType(),
        observedAt = occurredAt,
        observedElapsedMillis = observedElapsedMillis,
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

internal fun HealthReadingEntity.toDomain(): HealthReading =
    HealthReading(
        recordId = recordId,
        value = value,
        startTime = startTime,
        endTime = endTime,
        recordingMethod = recordingMethod.toRecordingMethod(),
        originPackage = originPackage,
    )

internal fun SleepSessionEntity.toDomain(): SleepSession =
    SleepSession(
        recordId = recordId,
        start = startAt,
        end = endAt,
        durationMillis = durationMillis,
        sleepMillis = sleepMillis,
        observedElapsedMillis = observedElapsedMillis,
        recordingMethod = recordingMethod.toRecordingMethod(),
        originPackage = originPackage,
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

private fun String.toGeofenceTransitionType(): GeofenceTransitionType =
    GeofenceTransitionType.entries.find { it.name == this } ?: GeofenceTransitionType.ENTER

internal fun String.toHealthMetric(): HealthMetric = HealthMetric.entries.find { it.name == this } ?: HealthMetric.STEPS

private fun String.toRecordingMethod(): RecordingMethod = RecordingMethod.entries.find { it.name == this } ?: RecordingMethod.UNKNOWN

private fun String.toAppEventType(): AppEventType = AppEventType.entries.find { it.name == this } ?: AppEventType.RESUMED
