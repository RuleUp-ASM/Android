package com.ruleup.verification.data.sync

import android.os.SystemClock
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.entity.DeviceClock
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/** 디바이스 시계/부팅 세션 채집. */
class BootSessionProvider
    @Inject
    constructor(
        private val settings: VerificationSettingsStore,
    ) {
        suspend fun currentClock(): DeviceClock {
            val elapsed = SystemClock.elapsedRealtime()
            val now = System.currentTimeMillis()
            val bootEpoch = now - elapsed

            val storedId = settings.bootSessionId()
            val storedAnchor = settings.bootEpochAnchor()
            val sessionId =
                if (storedId == null || storedAnchor == null || abs(bootEpoch - storedAnchor) > TOLERANCE_MS) {
                    UUID.randomUUID().toString().also { settings.setBootSession(it, bootEpoch) }
                } else {
                    storedId
                }

            return DeviceClock(
                deviceTimeMillis = now,
                elapsedRealtimeMillis = elapsed,
                bootSessionId = sessionId,
                timeZone = ZoneId.systemDefault().id,
            )
        }

        private companion object {
            // 부팅 epoch 흔들림 허용치(NTP 보정 흡수).
            const val TOLERANCE_MS = 5_000L
        }
    }
