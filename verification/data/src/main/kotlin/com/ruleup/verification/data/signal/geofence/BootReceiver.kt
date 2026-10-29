package com.ruleup.verification.data.signal.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruleup.verification.domain.repository.GeofenceRegister
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** OS 등록 유실 시 지오펜스 복원. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var geofenceRegister: GeofenceRegister

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (!requiresGeofenceRecovery(intent.action, intent.data?.schemeSpecificPart)) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geofenceRegister.reconcilePersisted(forceReregister = true)
            } catch (t: Throwable) {
                // 재등록 실패는 다음 콜드스타트 reconcile 이 보정한다.
            } finally {
                pending.finish()
            }
        }
    }
}

internal fun requiresGeofenceRecovery(
    action: String?,
    packageName: String?,
): Boolean =
    action == Intent.ACTION_BOOT_COMPLETED ||
        (action == Intent.ACTION_PACKAGE_DATA_CLEARED && packageName == "com.google.android.gms")
