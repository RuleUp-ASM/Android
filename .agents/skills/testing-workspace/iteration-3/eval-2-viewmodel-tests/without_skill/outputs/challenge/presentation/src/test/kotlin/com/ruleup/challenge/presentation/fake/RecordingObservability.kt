package com.ruleup.challenge.presentation.fake

import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ContextProvider
import com.ruleup.observability.domain.port.Policy
import com.ruleup.observability.domain.port.Sink

/** 나간 이벤트를 순서대로 담아 두는 출구. */
class RecordingSink : Sink {
    val events = mutableListOf<ObsEvent>()

    override fun emit(event: ObsEvent) {
        events += event
    }

    /** feature 이벤트만. 이 화면은 전부 [BusinessPayload.Custom] 으로 나간다. */
    val business: List<BusinessPayload.Custom>
        get() = events.map { it.payload }.filterIsInstance<BusinessPayload.Custom>()

    val names: List<String>
        get() = business.map { it.name }
}

/**
 * [Observability] 는 인터페이스가 아니라 파이프라인 본체라 포트만 테스트용으로 갈아끼운다.
 *
 * 프로필을 [BuildProfile.DEV] 로 두는 건 의도적이다 — 게이트 인자와 페이로드의 channel·severity·tag 가
 * 어긋나면 그 자리에서 실패해, 호출부가 채널을 잘못 적은 걸 테스트가 잡는다.
 */
fun recordingObservability(sink: RecordingSink) =
    Observability(
        clock =
            object : Clock {
                override fun epochMillis() = 0L

                override fun monotonicNanos() = 0L
            },
        contextProvider = ContextProvider { ObsContext(currentScreen = null) },
        profile = BuildProfile.DEV,
        policy = Policy { _, _, _ -> true },
        sink = sink,
    )
