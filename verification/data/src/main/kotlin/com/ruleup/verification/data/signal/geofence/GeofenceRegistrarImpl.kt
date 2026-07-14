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
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * GeofencingClient 로 활성 좌표를 OS 에 사전 등록(zero-touch presence, 명세 §2.1).
 * reconcile 은 차집합만 제거하고 새 목표 전체를 멱등 등록한다(재부팅 후 전부 재등록).
 *
 * 지오펜싱 API 호출은 모두 [Context.hasFineLocation] 가드 뒤에서 일어나고 SecurityException 을 삼키므로
 * 권한 lint(MissingPermission)를 클래스 단위로 억제한다.
 */
@SuppressLint("MissingPermission")
class GeofenceRegistrarImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val geofenceTargetDao: GeofenceTargetDao,
        private val gapRecorder: GapRecorder,
        private val settingsStore: VerificationSettingsStore,
    ) : GeofenceRegistrar {
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

        override suspend fun bind(target: GeofenceTarget) {
            // 다른 목표는 유지하고 이 requestId 만 멱등 등록/갱신(변경 시 재등록 포함, 명세 §5.4.3).
            if (context.hasFineLocation()) {
                registerAll(listOf(target))
            } else {
                recordNotRegistered()
            }
            geofenceTargetDao.upsertAll(listOf(target.toEntity()))
        }

        override suspend fun unbind(requestId: String) {
            runCatching { client.removeGeofences(listOf(requestId)).await() }
            geofenceTargetDao.deleteByRequestId(requestId)
        }

        override suspend fun clear() {
            val ids = geofenceTargetDao.all().map { it.requestId }
            if (ids.isNotEmpty()) {
                runCatching { client.removeGeofences(ids).await() }
            }
            geofenceTargetDao.clear()
        }

        // OS 등록 성공 시 재등록 시각을 남기고(§0.7 heartbeat), 실패(SecurityException: BACKGROUND 미허용 등)는
        // 삼키되 GEOFENCE_NOT_REGISTERED gap 으로 보고한다(전송 스펙 §0.5) — 다음 reconcile 이 재시도.
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
                    Geofence
                        .Builder()
                        .setRequestId(target.requestId)
                        .setCircularRegion(target.lat, target.lng, target.radiusM)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        // DWELL 이 OS 에서 직접 "체류 임계 도달"을 쏜다(명세 §2.1).
                        .setLoiteringDelay(target.dwellMinutes * MILLIS_PER_MINUTE)
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
