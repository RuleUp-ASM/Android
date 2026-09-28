package com.ruleup.verification.data.signal.geofence

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.common.toEntity
import com.ruleup.verification.data.db.geofence.GeofenceTargetDao
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.data.signal.common.GapRecorder
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.repository.GeofenceRegister
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** GeofencingClient 로 활성 좌표를 OS 에 사전 등록. */
@SuppressLint("MissingPermission")
class GeofenceRegisterImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val geofenceTargetDao: GeofenceTargetDao,
        private val gapRecorder: GapRecorder,
        private val settingsStore: VerificationSettingsStore,
    ) : GeofenceRegister {
        private val client by lazy { LocationServices.getGeofencingClient(context.applicationContext) }

        override suspend fun reconcile(targets: List<GeofenceTarget>) {
            // 권한 없으면 OS 등록 불가 → 목표만 보존하고 종료(허용 후 reconcile 이 재시도).
            if (!context.hasFineLocation()) {
                persist(targets)
                if (targets.isNotEmpty()) recordNotRegistered()
                return
            }

            val previous = geofenceTargetDao.all().mapTo(HashSet()) { it.requestId }
            val toRemove = GeofenceReconcile.toRemove(previous, targets)
            if (toRemove.isNotEmpty()) {
                runCatching { client.removeGeofences(toRemove).await() }
            }
            if (targets.isNotEmpty()) {
                registerAll(targets)
            }
            persist(targets)
        }

        override suspend fun reconcilePersisted() {
            reconcile(geofenceTargetDao.all().map { it.toDomain() })
        }

        override suspend fun bind(
            requestIdPrefix: String,
            targets: List<GeofenceTarget>,
        ) {
            // 이 멤버(prefix) 소속 기존 펜스 중 새 목록에 없는 것만 해제
            val newIds = targets.mapTo(HashSet()) { it.requestId }
            val stale =
                geofenceTargetDao
                    .byRequestIdPrefix(requestIdPrefix)
                    .map { it.requestId }
                    .filterNot { it in newIds }
            if (stale.isNotEmpty()) {
                runCatching { client.removeGeofences(stale).await() }
            }
            if (targets.isNotEmpty()) {
                if (context.hasFineLocation()) registerAll(targets) else recordNotRegistered()
            }
            geofenceTargetDao.deleteByRequestIdPrefix(requestIdPrefix)
            if (targets.isNotEmpty()) geofenceTargetDao.upsertAll(targets.map { it.toEntity() })
        }

        override suspend fun unbind(requestIdPrefix: String) {
            val ids = geofenceTargetDao.byRequestIdPrefix(requestIdPrefix).map { it.requestId }
            if (ids.isNotEmpty()) {
                runCatching { client.removeGeofences(ids).await() }
            }
            geofenceTargetDao.deleteByRequestIdPrefix(requestIdPrefix)
        }

        override suspend fun clear() {
            val ids = geofenceTargetDao.all().map { it.requestId }
            if (ids.isNotEmpty()) {
                runCatching { client.removeGeofences(ids).await() }
            }
            geofenceTargetDao.clear()
        }

        // 등록 성공 시 시각 저장, 실패 시 GEOFENCE_NOT_REGISTERED 기록.
        private suspend fun registerAll(targets: List<GeofenceTarget>) {
            runCatching { client.addGeofences(buildRequest(targets), geofencePendingIntent(context)).await() }
                .onSuccess { settingsStore.setLastGeofenceReregisterAt(System.currentTimeMillis()) }
                .onFailure { recordNotRegistered() }
        }

        private suspend fun recordNotRegistered() {
            val now = System.currentTimeMillis()
            gapRecorder.record(
                signalType = SIGNAL_TYPE_GEOFENCE,
                reason = GapReason.GEOFENCE_NOT_REGISTERED,
                fromMillis = now,
                toMillis = now,
                recoverable = true,
            )
        }

        private suspend fun persist(targets: List<GeofenceTarget>) {
            geofenceTargetDao.clear()
            if (targets.isNotEmpty()) geofenceTargetDao.upsertAll(targets.map { it.toEntity() })
        }

        private fun buildRequest(targets: List<GeofenceTarget>): GeofencingRequest {
            val fences =
                targets.map { target ->
                    val loiteringDelay = target.dwellMinutes * MILLIS_PER_MINUTE
                    Geofence
                        .Builder()
                        .setRequestId(target.requestId)
                        .setCircularRegion(target.lat, target.lng, target.radiusM)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        // DWELL 이 OS 에서 직접 "체류 임계 도달"을 쏜다.
                        .setLoiteringDelay(loiteringDelay)
                        .setNotificationResponsiveness(geofenceResponsivenessFor(loiteringDelay))
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER or
                                Geofence.GEOFENCE_TRANSITION_EXIT or
                                Geofence.GEOFENCE_TRANSITION_DWELL,
                        ).build()
                }
            return GeofencingRequest
                .Builder()
                .setInitialTrigger(
                    GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL,
                ).addGeofences(fences)
                .build()
        }

        companion object {
            private const val MILLIS_PER_MINUTE = 60_000
            private const val SIGNAL_TYPE_GEOFENCE = "GEOFENCE"
        }
    }

// 배칭 허용 기본치 5분(Google 권고 수준).
internal const val DEFAULT_GEOFENCE_RESPONSIVENESS_MS = 5 * 60_000

/** 통지 지연 허용치. */
internal fun geofenceResponsivenessFor(loiteringDelayMillis: Int): Int =
    if (loiteringDelayMillis > 0) {
        minOf(DEFAULT_GEOFENCE_RESPONSIVENESS_MS, loiteringDelayMillis)
    } else {
        DEFAULT_GEOFENCE_RESPONSIVENESS_MS
    }
