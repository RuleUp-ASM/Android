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

/** 재부팅 후 지오펜스 재등록. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var geofenceRegister: GeofenceRegister

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // LOCKED_BOOT_COMPLETED 는 받지 않는다
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geofenceRegister.reconcilePersisted()
            } catch (t: Throwable) {
                // 재등록 실패는 다음 콜드스타트 reconcile 이 보정한다.
            } finally {
                pending.finish()
            }
        }
    }
}
