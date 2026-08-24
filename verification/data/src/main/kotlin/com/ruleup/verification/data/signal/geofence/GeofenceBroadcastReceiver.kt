package com.ruleup.verification.data.signal.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.ruleup.verification.data.db.common.verificationDatabase
import com.ruleup.verification.data.db.geofence.GeofenceTransitionEntity
import com.ruleup.verification.data.sync.VerificationSyncSchedulerImpl
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 지오펜스 전이 수신(전송 스펙 §1). 즉시 Room geofence_transition 에 적재한다
 * (앱이 죽어도 보존 → sync 가 드레인). 에러(GEOFENCE_NOT_AVAILABLE=위치 꺼짐 등)는 무시한다.
 *
 * 벽시계(`observedAt`)와 monotonic 시각(`observedElapsedMillis`)을 **수신 시점에 함께** 찍는다.
 * 둘의 간격이 어긋나면 시각 조작이라 서버가 대조하는데(전송 스펙 §6.4), 나중에 재구성할 수 없는
 * 값이라 여기서 놓치면 영영 못 채운다.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val type = geofenceTransitionTypeOfCode(event.geofenceTransition) ?: return
        val fences = event.triggeringGeofences ?: return
        if (fences.isEmpty()) return

        // 위치가 없는 전이도 신호로서 유효하다(어느 펜스를 언제 넘었는지). 다만 정확도·mock 여부는
        // 지어내지 않고 null 로 둔다 — 0m·"mock 아님"으로 접으면 없던 사실이 판정에 들어간다.
        val location = event.triggeringLocation
        val occurredAt = location?.time ?: System.currentTimeMillis()
        val observedElapsedMillis = SystemClock.elapsedRealtime()
        val accuracy = location?.accuracy
        val isMock = location?.isMockCompat()

        val dao = verificationDatabase(context).geofenceTransitionDao()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fences.forEach { fence ->
                    dao.insert(
                        GeofenceTransitionEntity(
                            requestId = fence.requestId,
                            transition = type.name,
                            accuracy = accuracy,
                            isMock = isMock,
                            occurredAt = occurredAt,
                            observedElapsedMillis = observedElapsedMillis,
                        ),
                    )
                }
                // 적재 직후 expedited catch-up flush 를 걸어 다음 30분 주기를 기다리지 않고 전송(전송 스펙 §0.6).
                VerificationSyncSchedulerImpl.enqueueCatchUp(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

private fun geofenceTransitionTypeOfCode(code: Int): GeofenceTransitionType? =
    when (code) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransitionType.ENTER
        Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransitionType.EXIT
        Geofence.GEOFENCE_TRANSITION_DWELL -> GeofenceTransitionType.DWELL
        else -> null
    }
