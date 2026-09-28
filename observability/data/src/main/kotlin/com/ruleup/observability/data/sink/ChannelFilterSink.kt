package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink

/** 채널 라우팅 데코레이터. */
internal class ChannelFilterSink(
    private val channels: Set<Channel>,
    private val delegate: Sink,
) : Sink {
    override fun emit(event: ObsEvent) {
        if (event.channel in channels) delegate.emit(event)
    }

    override fun flush() = delegate.flush()
}
