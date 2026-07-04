package com.ruleup.verification.data.signal.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.ruleup.verification.data.db.GeofenceTransitionEntity
import com.ruleup.verification.data.db.verificationDatabase
import com.ruleup.verification.data.sync.VerificationSyncSchedulerImpl
import com.ruleup.verification.domain.entity.GeofenceTransitionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 지오펜스 전이 수신(명세 §2.1). 즉시 isMock 을 포함해 Room geofence_transition 에 적재한다
 * (앱이 죽어도 보존 → sync 가 드레인). 에러(GEOFENCE_NOT_AVAILABLE=위치 꺼짐 등)는 무시한다.
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

        val location = event.triggeringLocation
        val occurredAt = location?.time ?: System.currentTimeMillis()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val accuracy = location?.accuracy ?: 0f
        val isMock = location?.isMockCompat() ?: false

        val dao = verificationDatabase(context).geofenceTransitionDao()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fences.forEach { fence ->
                    dao.insert(
                        GeofenceTransitionEntity(
                            requestId = fence.requestId,
                            transition = type.name,
                            lat = lat,
                            lng = lng,
                            accuracy = accuracy,
                            isMock = isMock,
                            occurredAt = occurredAt,
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
