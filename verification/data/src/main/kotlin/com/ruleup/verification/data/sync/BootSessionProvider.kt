package com.ruleup.verification.data.sync

import android.os.SystemClock
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.entity.DeviceClock
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/**
 * 디바이스 시계/부팅 세션 채집(전송 스펙 §0.1·§6.4 시각 위조 방어). `currentTimeMillis - elapsedRealtime`
 * 는 한 부팅 세션 안에서 일정하므로, 앵커가 [TOLERANCE_MS] 넘게 벌어지면 재부팅·시각 점프로 보고 세션
 * id 를 새로 발급한다 — 서버는 같은 세션 안의 monotonic/wall-clock 불일치로 조작을 의심한다.
 */
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
            // 부팅 epoch 흔들림 허용치(NTP 보정 흡수). 초과 시 새 부팅 세션으로 간주.
            const val TOLERANCE_MS = 5_000L
        }
    }
