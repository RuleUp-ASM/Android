package com.ruleup.observability.data.sink

import com.amplitude.android.Amplitude
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink
import javax.inject.Provider

internal class AmplitudeSink(
    private val amplitude: Provider<Amplitude>,
) : Sink {
    override fun emit(event: ObsEvent) {
        amplitude.get().track(
            AmplitudeEventMapper.eventName(event.payload),
            AmplitudeEventMapper.toProperties(event),
        )
    }
}
