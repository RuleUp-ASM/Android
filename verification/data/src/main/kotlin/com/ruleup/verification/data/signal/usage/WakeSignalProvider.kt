package com.ruleup.verification.data.signal.usage

import android.app.KeyguardManager
import android.content.Context
import com.ruleup.verification.data.db.usage.UsageEventDao
import com.ruleup.verification.domain.entity.ScreenEventType
import com.ruleup.verification.domain.entity.VerificationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject

/**
 * 기상 신호 조립(전송 스펙 §4). 화면 이벤트 버퍼에서 **당일 첫** 잠금해제·화면 켜짐만 뽑아 올린다.
 *
 * 하루 경계는 KST 로 고정한다 — 사용자가 기기 시간대를 바꿔도 서버 판정 기준은 KST 라
 * (공통 테크스펙 「날짜 귀속」), 기기 시간대로 자르면 클라와 서버가 서로 다른 날의 첫 시각을 본다.
 */
class WakeSignalProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val usageEventDao: UsageEventDao,
    ) {
        /** 당일 첫 시각이 하나도 없으면 null — 보낼 것이 없는 신호는 배치에 넣지 않는다. */
        suspend fun collect(): VerificationSignal.Wake? {
            val since = startOfTodayKst()
            val firstUnlock = usageEventDao.firstScreenEventAt(ScreenEventType.UNLOCK.name, since)
            val firstScreenOn = usageEventDao.firstScreenEventAt(ScreenEventType.SCREEN_ON.name, since)
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

/** 잠금이 설정되지 않은 기기는 잠금해제 이벤트가 영영 나오지 않는다(전송 스펙 §4). */
private fun Context.isDeviceSecure(): Boolean = getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

private val KST: ZoneId = ZoneId.of("Asia/Seoul")
