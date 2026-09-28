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

/** 지오펜스 전이 수신. */
@dagger.hilt.android.AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    @javax.inject.Inject
    lateinit var syncGate: com.ruleup.verification.data.sync.SyncGate

    @javax.inject.Inject
    lateinit var tokens: com.ruleup.domain.token.TokenRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val type = geofenceTransitionTypeOfCode(event.geofenceTransition) ?: return
        val fences = event.triggeringGeofences ?: return
        if (fences.isEmpty()) return

        // 위치가 없는 전이도 신호로서 유효하다(어느 펜스를 언제 넘었는지).
        val location = event.triggeringLocation
        val occurredAt = location?.time ?: System.currentTimeMillis()
        val observedElapsedMillis = SystemClock.elapsedRealtime()
        val accuracy = location?.accuracy
        val isMock = location?.isMockCompat()

        val dao = verificationDatabase(context).geofenceTransitionDao()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncGate.exclusively {
                    val userId = tokens.getUserId() ?: return@exclusively
                    val registered =
                        verificationDatabase(context)
                            .geofenceTargetDao()
                            .all()
                            .map { it.requestId }
                            .toSet()
                    fences.filter { it.requestId.startsWith("$userId#") && it.requestId in registered }.forEach { fence ->
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
                    // 적재 직후 expedited catch-up flush 를 걸어 다음 30분 주기를 기다리지 않고 전송.
                    VerificationSyncSchedulerImpl.enqueueCatchUp(context.applicationContext)
                }
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
