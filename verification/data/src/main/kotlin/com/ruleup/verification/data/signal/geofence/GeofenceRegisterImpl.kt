package com.ruleup.verification.data.signal.geofence

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

        private val lock = Mutex()

        private val mostRecentUserStopAt: Long? by lazy {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                null
            } else {
                try {
                    context
                        .getSystemService(ActivityManager::class.java)
                        .getHistoricalProcessExitReasons(null, 0, 0)
                        .filter { it.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
                        .maxOfOrNull { it.timestamp }
                } catch (_: SecurityException) {
                    System.currentTimeMillis()
                } catch (_: IllegalArgumentException) {
                    System.currentTimeMillis()
                }
            }
        }

        private val coordinator =
            GeofenceRegistrationCoordinator(
                readRegistered = { settingsStore.registeredGeofences() },
                writeRegistered = { settingsStore.setRegisteredGeofences(it) },
                add = { registerAll(it) },
                remove = { ids -> runCatching { client.removeGeofences(ids).await() }.isSuccess },
            )

        override suspend fun reconcile(targets: List<GeofenceTarget>) = lock.withLock { reconcileLocked(targets) }

        override suspend fun reconcilePersisted(forceReregister: Boolean) =
            lock.withLock {
                if (forceReregister) settingsStore.invalidateRegisteredGeofences()
                reconcileLocked(geofenceTargetDao.all().map { it.toDomain() })
            }

        override suspend fun bind(
            requestIdPrefix: String,
            targets: List<GeofenceTarget>,
        ) = lock.withLock {
            val retained = geofenceTargetDao.all().map { it.toDomain() }.filterNot { it.requestId.startsWith(requestIdPrefix) }
            reconcileLocked(retained + targets)
        }

        override suspend fun unbind(requestIdPrefix: String) =
            lock.withLock {
                val retained = geofenceTargetDao.all().map { it.toDomain() }.filterNot { it.requestId.startsWith(requestIdPrefix) }
                reconcileLocked(retained)
            }

        override suspend fun clear() = lock.withLock { reconcileLocked(emptyList()) }

        private suspend fun reconcileLocked(targets: List<GeofenceTarget>) {
            val lastRegisteredAt = settingsStore.lastGeofenceReregisterAt()

            @Suppress("DEPRECATION")
            val lastPackageUpdateAt = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            if (needsRegistrationRecovery(lastRegisteredAt, lastPackageUpdateAt, mostRecentUserStopAt, hasGeofencePendingIntent(context))) {
                settingsStore.invalidateRegisteredGeofences()
            }
            persist(targets)
            if (!context.hasFineLocation()) {
                settingsStore.invalidateRegisteredGeofences()
                if (targets.isNotEmpty()) recordNotRegistered()
                coordinator.reconcile(targets, canRegister = false)
                return
            }
            coordinator.reconcile(targets)
        }

        private suspend fun registerAll(targets: List<GeofenceTarget>): Boolean =
            runCatching { client.addGeofences(buildRequest(targets), geofencePendingIntent(context)).await() }
                .onSuccess { settingsStore.setLastGeofenceReregisterAt(System.currentTimeMillis()) }
                .onFailure { recordNotRegistered() }
                .isSuccess

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
                    GeofencingRequest.INITIAL_TRIGGER_ENTER,
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
