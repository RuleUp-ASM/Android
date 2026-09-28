package com.ruleup.logging.data.sink

import android.util.Log
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter

/** Logcat 출구. */
internal class LogcatBizShooter : BizLogShooter {
    override suspend fun shoot(log: BizLog) {
        val attrs =
            log.event.attrs.entries.entries
                .joinToString(", ") { (key, value) -> "${key.raw}=$value" }
        Log.d(TAG, "${log.event.name} @${log.screen ?: "-"} user=${log.userId ?: "-"} {$attrs}")
    }

    private companion object {
        const val TAG = "BIZLOG"
    }
}
