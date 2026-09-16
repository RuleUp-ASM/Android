package com.ruleup.logging.data.sink

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.common.Logger
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogConfig
import com.ruleup.logging.domain.BizLogShooter

/**
 * Amplitude 출구. Firebase 와 **병행**한다 — 같은 이벤트가 두 곳에 쌓이므로 집계할 때 출처를 섞지 않는다.
 *
 * **autocapture 는 SDK 기본값(세션만) 그대로 둔다.** 화면 조회를 자동 수집하면 우리가 남기는
 * `screen_view` 와 대시보드에서 구분이 안 된다.
 *
 * 업로드는 SDK 자체 큐가 비동기로 하므로 전송 실패는 [shoot] 으로 관측되지 않는다.
 */
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
