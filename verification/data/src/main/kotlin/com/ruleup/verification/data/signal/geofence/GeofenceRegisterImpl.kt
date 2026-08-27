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

/**
 * GeofencingClient 로 활성 좌표를 OS 에 사전 등록(zero-touch presence, 명세 §2.1).
 * reconcile 은 차집합만 제거하고 새 목표 전체를 멱등 등록한다(재부팅 후 전부 재등록).
 *
 * 등록만 [Context.hasFineLocation] 가드 뒤에 있다. 해제는 권한이 필요 없고 전부 runCatching 안이라
 * 가드를 두지 않는다 — 그래서 권한 lint(MissingPermission)를 클래스 단위로 억제한다.
 */
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
            // 이 멤버(prefix) 소속 기존 펜스 중 새 목록에 없는 것만 해제 — 다른 멤버 목표는 유지(명세 §5.4.3).
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
                    val loiteringDelay = target.dwellMinutes * MILLIS_PER_MINUTE
                    Geofence
                        .Builder()
                        .setRequestId(target.requestId)
                        .setCircularRegion(target.lat, target.lng, target.radiusM)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        // DWELL 이 OS 에서 직접 "체류 임계 도달"을 쏜다(명세 §2.1).
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

// 배칭 허용 기본치 5분(Google 권고 수준). 30분 주기 전송보다 한참 짧아 전달 지연에 묻힌다.
internal const val DEFAULT_GEOFENCE_RESPONSIVENESS_MS = 5 * 60_000

/**
 * 통지 지연 허용치 — 클수록 Play Services 가 지오펜스 검사를 다른 위치 작업과 묶어(배칭)
 * 위치 하드웨어를 덜 깨운다. 지정하지 않으면 기본 0(=최대한 빨리)이라 배칭이 아예 꺼진다.
 *
 * 늦게 배달돼도 판정은 멀쩡하다 — 전이 시각은 배달 시점이 아니라 fix 시각(`location.time`)에서
 * 오고, 전송은 어차피 30분 주기다.
 *
 * 다만 [loiteringDelayMillis] 를 넘기면 DWELL 통지가 체류 임계 도달보다 늦게 잡힐 수 있어 체류 목표가
 * 짧은 방이 손해를 본다. `dwellMinutes` 는 서버가 챌린지별로 내려주는 값이라 하한을 가정할 수 없으므로,
 * 기본값을 쓰되 체류 목표가 그보다 짧으면 거기에 맞춘다. 체류 목표가 없는 펜스(0)는 DWELL 을 쏘지
 * 않으므로 깎을 이유가 없다.
 */
internal fun geofenceResponsivenessFor(loiteringDelayMillis: Int): Int =
    if (loiteringDelayMillis > 0) {
        minOf(DEFAULT_GEOFENCE_RESPONSIVENESS_MS, loiteringDelayMillis)
    } else {
        DEFAULT_GEOFENCE_RESPONSIVENESS_MS
    }
