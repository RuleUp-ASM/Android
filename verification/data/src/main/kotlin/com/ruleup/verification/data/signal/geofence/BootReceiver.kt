package com.ruleup.verification.data.signal.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.common.verificationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 재부팅 후 지오펜스 재등록(명세 §2.3 휘발 대응). OS 등록은 부팅 시 소멸하므로 로컬에 보존한
 * 목표(geofence_target)를 읽어 전부 재등록한다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val targetDao = verificationDatabase(appContext).geofenceTargetDao()
        val registrar = GeofenceRegistrarImpl(appContext, targetDao)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targets = targetDao.all().map { it.toDomain() }
                if (targets.isNotEmpty()) registrar.reconcile(targets)
            } catch (t: Throwable) {
                // 재등록 실패는 다음 콜드스타트 reconcile 이 보정한다.
            } finally {
                pending.finish()
            }
        }
    }
}
