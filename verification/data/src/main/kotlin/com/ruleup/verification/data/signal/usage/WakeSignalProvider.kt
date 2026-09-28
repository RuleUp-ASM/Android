package com.ruleup.verification.data.signal.usage

import android.app.KeyguardManager
import android.content.Context
import com.ruleup.verification.data.db.usage.UsageEventDao
import com.ruleup.verification.data.db.usage.UsageEventType
import com.ruleup.verification.domain.entity.VerificationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject

/** 기상 신호 조립. */
class WakeSignalProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val usageEventDao: UsageEventDao,
    ) {
        /** 당일 첫 시각이 하나도 없으면 null */
        suspend fun collect(): VerificationSignal.Wake? {
            val since = startOfTodayKst()
            val firstUnlock = usageEventDao.firstScreenEventAt(UsageEventType.UNLOCK, since)
            val firstScreenOn = usageEventDao.firstScreenEventAt(UsageEventType.SCREEN_ON, since)
            if (firstUnlock == null && firstScreenOn == null) return null

            return VerificationSignal.Wake(
                firstUnlock = firstUnlock,
                firstScreenOn = firstScreenOn,
                deviceSecure = context.isDeviceSecure(),
            )
        }
    }

private fun startOfTodayKst(): Long =
    java.time.LocalDate
        .now(KST)
        .atStartOfDay(KST)
        .toInstant()
        .toEpochMilli()

/** 잠금이 설정되지 않은 기기는 잠금해제 이벤트가 영영 나오지 않는다. */
private fun Context.isDeviceSecure(): Boolean = getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

private val KST: ZoneId = ZoneId.of("Asia/Seoul")
