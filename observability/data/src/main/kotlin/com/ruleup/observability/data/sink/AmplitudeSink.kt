package com.ruleup.observability.data.sink

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.common.Logger
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.AmplitudeApiKey
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.port.Sink

/**
 * Amplitude 출구. Firebase 와 **병행**한다 — 같은 이벤트가 두 곳에 쌓이므로 집계할 때 출처를 섞지 않는다.
 *
 * **autocapture 는 SDK 기본값(세션만) 그대로 둔다.** 화면 조회를 자동 수집하면 이미 `screen_view` 로
 * 파이프라인을 타는 우리 이벤트와 대시보드에서 구분이 안 된다.
 *
 * 업로드는 SDK 자체 큐가 비동기로 하므로 전송 실패는 [emit] 으로 관측되지 않는다.
 */
internal class AmplitudeSink(
    private val context: Context,
    private val apiKey: AmplitudeApiKey,
    private val profile: BuildProfile,
) : Sink {
    // 생성 시점에 SDK 가 저장소·네트워크를 건드리므로 첫 이벤트가 날 때까지 미룬다.
    private val amplitude by lazy {
        Amplitude(
            Configuration(
                apiKey = apiKey.value,
                context = context,
            ),
        ).also {
            // 기본값(WARN)이면 전송 성공이 아무 흔적도 남기지 않아 "올라가고 있나"를 볼 방법이 없다.
            if (profile.isDebuggable) it.logger.logMode = Logger.LogMode.DEBUG
        }
    }

    override fun emit(event: ObsEvent) {
        amplitude.track(
            AmplitudeEventMapper.eventName(event.payload),
            AmplitudeEventMapper.toProperties(event),
        )
    }
}
