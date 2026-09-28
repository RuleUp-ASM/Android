package com.ruleup.observability.data.sink

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.common.Logger
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.AmplitudeApiKey
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.port.Sink

/** Amplitude 출구. */
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
