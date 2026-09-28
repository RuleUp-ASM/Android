package com.ruleup.logging.data.sink

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.common.Logger
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogConfig
import com.ruleup.logging.domain.BizLogShooter

/** Amplitude 출구. */
internal class AmplitudeBizShooter(
    private val context: Context,
    private val config: BizLogConfig,
) : BizLogShooter {
    // 생성 시점에 SDK 가 저장소·네트워크를 건드리므로 첫 이벤트가 날 때까지 미룬다.
    private val amplitude by lazy {
        Amplitude(
            Configuration(
                apiKey = config.amplitudeApiKey,
                context = context,
            ),
        ).also {
            // 기본값(WARN)이면 전송 성공이 아무 흔적도 남기지 않아 "올라가고 있나"를 볼 방법이 없다.
            if (config.debuggable) it.logger.logMode = Logger.LogMode.DEBUG
        }
    }

    override suspend fun shoot(log: BizLog) {
        amplitude.track(BizEventMapper.eventName(log), BizEventMapper.toProperties(log))
    }
}
