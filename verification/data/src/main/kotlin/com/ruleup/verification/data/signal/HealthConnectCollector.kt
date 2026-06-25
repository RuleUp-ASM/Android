package com.ruleup.verification.data.signal

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.ruleup.verification.data.db.HealthReadingDao
import com.ruleup.verification.data.db.HealthReadingEntity
import com.ruleup.verification.data.db.SleepSegmentDao
import com.ruleup.verification.data.db.SleepSegmentEntity
import com.ruleup.verification.domain.entity.HealthDeviceType
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.entity.RecordingMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 움직임·수면(HEALTH·SLEEP) 온디바이스 수집(명세 §8). Health Connect 에서 **결과값+신뢰 메타데이터만**
 * 읽어(원시 트랙 미전송, §1.5) 로컬 버퍼에 적재한다. 하루치 누적은 매 sync 마다 최신 스냅샷으로 교체한다.
 *
 * 호출은 반드시 API 26 가드 뒤에서(아래 [capture] 의 [RequiresApi]). connect-client minSdk 26 +
 * java.time 사용이라 26 미만 기기에선 이 클래스의 메서드를 실행하지 않는다(생성자·필드는 java.time 무관).
 */
class HealthConnectCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val healthReadingDao: HealthReadingDao,
        private val sleepSegmentDao: SleepSegmentDao,
    ) {
        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun capture(
            healthTargets: Set<HealthTarget>,
            sleepRequested: Boolean,
        ) {
            if (healthTargets.isEmpty() && !sleepRequested) return
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

        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun captureHealth(
            client: HealthConnectClient,
            granted: Set<String>,
            targets: Set<HealthTarget>,
        ) {
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val date = LocalDate.now(zone).toString()
            val range = TimeRangeFilter.between(todayStart, now)

            val rows = ArrayList<HealthReadingEntity>()
            for (target in targets) {
                try {
                    when (target.metric) {
                        HealthMetric.DISTANCE -> rows += readDistance(client, granted, range, date, target.exerciseType)
                        HealthMetric.STEPS -> rows += readSteps(client, granted, range, date, target.exerciseType)
                        HealthMetric.EXERCISE_DURATION -> rows += readExercise(client, granted, range, date, target.exerciseType)
                    }
                } catch (e: SecurityException) {
                    // 권한 회수 등 — 해당 metric 만 건너뛴다.
                } catch (e: IllegalStateException) {
                    // HC 클라이언트 일시 오류 — 다음 sync 에서 재시도.
                }
            }
            // 미전송 스냅샷을 최신값으로 교체(같은 날 누적이 sync 마다 갱신, 명세 §8).
            healthReadingDao.deleteUntagged()
            if (rows.isNotEmpty()) healthReadingDao.insertAll(rows)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun readDistance(
            client: HealthConnectClient,
            granted: Set<String>,
            range: TimeRangeFilter,
            date: String,
            exerciseType: String?,
        ): List<HealthReadingEntity> {
            if (HealthPermission.getReadPermission(DistanceRecord::class) !in granted) return emptyList()
            return client
                .readRecords(ReadRecordsRequest(DistanceRecord::class, range))
                .records
                .map { rec ->
                    reading(
                        metric = HealthMetric.DISTANCE,
                        value = rec.distance.inKilometers,
                        unit = "km",
                        exerciseType = exerciseType,
                        metadata = rec.metadata,
                        at = rec.endTime,
                        date = date,
                    )
                }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun readSteps(
            client: HealthConnectClient,
            granted: Set<String>,
            range: TimeRangeFilter,
            date: String,
            exerciseType: String?,
        ): List<HealthReadingEntity> {
            if (HealthPermission.getReadPermission(StepsRecord::class) !in granted) return emptyList()
            return client
                .readRecords(ReadRecordsRequest(StepsRecord::class, range))
                .records
                .map { rec ->
                    reading(
                        metric = HealthMetric.STEPS,
                        value = rec.count.toDouble(),
                        unit = "count",
                        exerciseType = exerciseType,
                        metadata = rec.metadata,
                        at = rec.endTime,
                        date = date,
                    )
                }
        }

        @RequiresApi(Build.VERSION_CODES.O)
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
                        unit = "min",
                        exerciseType = typeName,
                        metadata = rec.metadata,
                        at = rec.endTime,
                        date = date,
                    )
                }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private suspend fun captureSleep(
            client: HealthConnectClient,
            granted: Set<String>,
        ) {
            if (HealthPermission.getReadPermission(SleepSessionRecord::class) !in granted) return
            val now = Instant.now()
            // 익일 배치라 지난밤을 포함하도록 36h 윈도우(명세 §6.2 SLEEP lag).
            val range = TimeRangeFilter.between(now.minus(Duration.ofHours(SLEEP_WINDOW_HOURS)), now)
            val rows = ArrayList<SleepSegmentEntity>()
            try {
                client
                    .readRecords(ReadRecordsRequest(SleepSessionRecord::class, range))
                    .records
                    .forEach { session ->
                        if (session.stages.isEmpty()) {
                            rows +=
                                SleepSegmentEntity(
                                    startAt = session.startTime.toEpochMilli(),
                                    endAt = session.endTime.toEpochMilli(),
                                    status = "SLEEPING",
                                    occurredAt = session.endTime.toEpochMilli(),
                                )
                        } else {
                            session.stages.forEach { stage ->
                                rows +=
                                    SleepSegmentEntity(
                                        startAt = stage.startTime.toEpochMilli(),
                                        endAt = stage.endTime.toEpochMilli(),
                                        status = sleepStageName(stage.stage),
                                        occurredAt = stage.endTime.toEpochMilli(),
                                    )
                            }
                        }
                    }
            } catch (e: SecurityException) {
                return
            } catch (e: IllegalStateException) {
                return
            }
            sleepSegmentDao.deleteUntagged()
            if (rows.isNotEmpty()) sleepSegmentDao.insertAll(rows)
        }

        private fun reading(
            metric: HealthMetric,
            value: Double,
            unit: String,
            exerciseType: String?,
            metadata: Metadata,
            at: Instant,
            date: String,
        ): HealthReadingEntity =
            HealthReadingEntity(
                metric = metric.name,
                value = value,
                unit = unit,
                exerciseType = exerciseType,
                dataOrigin = metadata.dataOrigin.packageName,
                recordingMethod = recordingMethodOf(metadata.recordingMethod).name,
                deviceType = deviceTypeOf(metadata.device?.type).name,
                date = date,
                occurredAt = at.toEpochMilli(),
            )

        private fun recordingMethodOf(method: Int): RecordingMethod =
            when (method) {
                Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> RecordingMethod.ACTIVE
                Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> RecordingMethod.AUTO
                Metadata.RECORDING_METHOD_MANUAL_ENTRY -> RecordingMethod.MANUAL
                else -> RecordingMethod.UNKNOWN
            }

        private fun deviceTypeOf(type: Int?): HealthDeviceType =
            when (type) {
                Device.TYPE_PHONE -> HealthDeviceType.PHONE
                Device.TYPE_WATCH -> HealthDeviceType.WATCH
                else -> HealthDeviceType.UNKNOWN
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

        private fun sleepStageName(stage: Int): String =
            when (stage) {
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                -> "AWAKE"
                SleepSessionRecord.STAGE_TYPE_LIGHT -> "LIGHT"
                SleepSessionRecord.STAGE_TYPE_DEEP -> "DEEP"
                SleepSessionRecord.STAGE_TYPE_REM -> "REM"
                SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "OUT_OF_BED"
                SleepSessionRecord.STAGE_TYPE_SLEEPING -> "SLEEPING"
                else -> "UNKNOWN"
            }

        private companion object {
            const val SLEEP_WINDOW_HOURS = 36L
        }
    }
