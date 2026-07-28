package com.ruleup.observability.data.sink

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink

/**
 * 채널 라우팅 데코레이터. 관심 채널의 이벤트만 [delegate] 로 흘린다.
 *
 * 도메인의 `Sink` 에서 `channels` 필드를 뺀 자리를 대신한다. 필드로 되살리지 않고 데코레이터로
 * 두면 **어느 백엔드가 어느 채널을 받는지가 배선 코드에 그대로 드러난다.**
 */
internal class ChannelFilterSink(
    private val channels: Set<Channel>,
    private val delegate: Sink,
) : Sink {
    override fun emit(event: ObsEvent) {
        if (event.channel in channels) delegate.emit(event)
    }

    override fun flush() = delegate.flush()
}
