package com.ruleup.logging.data.sink

import android.util.Log
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter

/**
 * Logcat 출구. **프로덕션에는 배선하지 않는다** — 무엇이 어떤 값으로 나가는지 기기에서 바로 읽으려는
 * 개발용이다(`BIZLOG` 로 거르면 된다).
 */
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
