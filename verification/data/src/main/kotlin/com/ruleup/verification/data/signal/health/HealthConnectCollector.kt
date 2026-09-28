package com.ruleup.verification.data.signal.health

import android.content.Context
import android.os.SystemClock
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.ruleup.verification.data.db.health.HealthReadingDao
import com.ruleup.verification.data.db.health.HealthReadingEntity
import com.ruleup.verification.data.db.health.SleepSessionDao
import com.ruleup.verification.data.db.health.SleepSessionEntity
import com.ruleup.verification.data.signal.common.GapRecorder
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.entity.RecordingMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** 움직임·수면(HEALTH·SLEEP) 온디바이스 수집. */
class HealthConnectCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val healthReadingDao: HealthReadingDao,
        private val sleepSessionDao: SleepSessionDao,
        private val gapRecorder: GapRecorder,
    ) {
        suspend fun capture(
            healthTargets: Set<HealthTarget>,
            sleepRequested: Boolean,
        ) {
            if (healthTargets.isEmpty() && !sleepRequested) return

            // HC 가용성 분기
            when (
                androidx.health.connect.client.HealthConnectClient
                    .getSdkStatus(context)
            ) {
                androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE -> {
                    recordHealthGaps(healthTargets, sleepRequested, GapReason.SIGNAL_UNSUPPORTED_DEVICE, recoverable = false)
                    return
                }
                androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    recordHealthGaps(healthTargets, sleepRequested, GapReason.HC_PROVIDER_UPDATE_REQUIRED, recoverable = true)
                    return
                }
                else -> Unit
            }

            val client = HealthPermissions.clientOrNull(context) ?: return
            val granted =
                try {
                    client.permissionController.getGrantedPermissions()
                } catch (e: SecurityException) {
                    return
                }

            if (healthTargets.isNotEmpty()) captureHealth(client, granted, healthTargets)
            if (sleepRequested) captureSleep(client, granted)
        }

        private suspend fun recordHealthGaps(
            healthTargets: Set<HealthTarget>,
            sleepRequested: Boolean,
            reason: GapReason,
            recoverable: Boolean,
        ) {
            val now = System.currentTimeMillis()
            val from = now - GAP_WINDOW_MS
            if (healthTargets.isNotEmpty()) gapRecorder.record("HEALTH", reason, from, now, recoverable)
            if (sleepRequested) gapRecorder.record("SLEEP", reason, from, now, recoverable)
        }

        private suspend fun captureHealth(
            client: HealthConnectClient,
            granted: Set<String>,
            targets: Set<HealthTarget>,
        ) {
            val zone = com.ruleup.domain.time.ServiceDate.ZONE
            val now = Instant.now()
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val date = LocalDate.now(zone).toString()
            val range = TimeRangeFilter.between(todayStart, now)

            val rows = ArrayList<HealthReadingEntity>()
            for (target in targets) {
                try {
                    when (target.metric) {
                        HealthMetric.DISTANCE -> rows += readDistance(client, granted, range, date)
                        HealthMetric.STEPS -> rows += readSteps(client, granted, range, date)
                        HealthMetric.EXERCISE_DURATION -> rows += readExercise(client, granted, range, date, target.exerciseType)
                    }
                } catch (e: SecurityException) {
                    // 권한 회수 등
                } catch (e: IllegalStateException) {
                    // HC 클라이언트 일시 오류
                }
            }
            // 미전송 스냅샷을 최신값으로 교체.
            healthReadingDao.deleteUntagged()
            if (rows.isNotEmpty()) healthReadingDao.insertAll(rows)
        }

        private suspend fun readDistance(
            client: HealthConnectClient,
            granted: Set<String>,
            range: TimeRangeFilter,
            date: String,
        ): List<HealthReadingEntity> {
            if (HealthPermission.getReadPermission(DistanceRecord::class) !in granted) return emptyList()
            return client
                .readRecords(ReadRecordsRequest(DistanceRecord::class, range))
                .records
                .map { rec ->
                    reading(
                        metric = HealthMetric.DISTANCE,
                        value = rec.distance.inKilometers,
                        metadata = rec.metadata,
                        startTime = rec.startTime,
                        endTime = rec.endTime,
                        date = date,
                    )
                }
        }

        private suspend fun readSteps(
            client: HealthConnectClient,
            granted: Set<String>,
            range: TimeRangeFilter,
            date: String,
        ): List<HealthReadingEntity> {
            if (HealthPermission.getReadPermission(StepsRecord::class) !in granted) return emptyList()
            return client
                .readRecords(ReadRecordsRequest(StepsRecord::class, range))
                .records
                .map { rec ->
                    reading(
                        metric = HealthMetric.STEPS,
                        value = rec.count.toDouble(),
                        metadata = rec.metadata,
                        startTime = rec.startTime,
                        endTime = rec.endTime,
                        date = date,
                    )
                }
        }

        private suspend fun readExercise(
            client: HealthConnectClient,
            granted: Set<String>,
            range: TimeRangeFilter,
            date: String,
            exerciseType: String?,
        ): List<HealthReadingEntity> {
            if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) !in granted) return emptyList()
            return client
                .readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range))
                .records
                .mapNotNull { rec ->
                    val typeName = exerciseTypeName(rec.exerciseType)
                    // 챌린지가 특정 운동만 요구하면(예: RUNNING) 그 외 세션은 제외.
                    if (exerciseType != null && typeName != exerciseType) return@mapNotNull null
                    val minutes = Duration.between(rec.startTime, rec.endTime).toMinutes().toDouble()
                    reading(
                        metric = HealthMetric.EXERCISE_DURATION,
                        value = minutes,
                        metadata = rec.metadata,
                        startTime = rec.startTime,
                        endTime = rec.endTime,
                        date = date,
                    )
                }
        }

        private suspend fun captureSleep(
            client: HealthConnectClient,
            granted: Set<String>,
        ) {
            if (HealthPermission.getReadPermission(SleepSessionRecord::class) !in granted) return
            val now = Instant.now()
            // 익일 배치라 지난밤을 포함하도록 36h 윈도우.
            val range = TimeRangeFilter.between(now.minus(Duration.ofHours(SLEEP_WINDOW_HOURS)), now)
            val rows = ArrayList<SleepSessionEntity>()
            try {
                client
                    .readRecords(ReadRecordsRequest(SleepSessionRecord::class, range))
                    .records
                    .forEach { session ->
                        val start = session.startTime.toEpochMilli()
                        val end = session.endTime.toEpochMilli()
                        rows +=
                            SleepSessionEntity(
                                recordId = session.metadata.id,
                                startAt = start,
                                endAt = end,
                                durationMillis = end - start,
                                sleepMillis = session.sleepMillisOrNull(),
                                observedElapsedMillis = SystemClock.elapsedRealtime(),
                                originPackage = session.metadata.dataOrigin.packageName,
                                recordingMethod = recordingMethodOf(session.metadata.recordingMethod),
                                occurredAt = end,
                            )
                    }
            } catch (e: SecurityException) {
                return
            } catch (e: IllegalStateException) {
                return
            }
            sleepSessionDao.deleteUntagged()
            if (rows.isNotEmpty()) sleepSessionDao.insertAll(rows)
        }

        /** 전송 계약에 있는 값만 담는다. */
        private fun reading(
            metric: HealthMetric,
            value: Double,
            metadata: Metadata,
            startTime: Instant,
            endTime: Instant,
            date: String,
        ): HealthReadingEntity =
            HealthReadingEntity(
                recordId = metadata.id,
                metric = metric,
                value = value,
                startTime = startTime.toEpochMilli(),
                endTime = endTime.toEpochMilli(),
                originPackage = metadata.dataOrigin.packageName,
                recordingMethod = recordingMethodOf(metadata.recordingMethod),
                date = date,
                occurredAt = endTime.toEpochMilli(),
            )

        private fun recordingMethodOf(method: Int): RecordingMethod =
            when (method) {
                Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> RecordingMethod.ACTIVE
                Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> RecordingMethod.AUTO
                Metadata.RECORDING_METHOD_MANUAL_ENTRY -> RecordingMethod.MANUAL
                else -> RecordingMethod.UNKNOWN
            }

        private fun exerciseTypeName(type: Int): String =
            when (type) {
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
                -> "RUNNING"
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "WALKING"
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
                -> "BIKING"
                else -> "OTHER"
            }

        /** 실제 수면 구간 합. */
        private fun SleepSessionRecord.sleepMillisOrNull(): Long? {
            if (stages.isEmpty()) return null
            return stages
                .filterNot { it.stage in AWAKE_STAGES }
                .sumOf { Duration.between(it.startTime, it.endTime).toMillis() }
        }

        private companion object {
            const val SLEEP_WINDOW_HOURS = 36L
            const val GAP_WINDOW_MS = 30L * 60 * 1000

            val AWAKE_STAGES =
                setOf(
                    SleepSessionRecord.STAGE_TYPE_AWAKE,
                    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
                )
        }
    }
