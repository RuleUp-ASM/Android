package com.ruleup.observability.domain.api

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.model.TtiOutcome
import com.ruleup.observability.domain.model.TtiPage
import com.ruleup.observability.domain.model.TtiPhase
import com.ruleup.observability.domain.model.TtiTimeline
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ResourceSampler

/**
 * 화면 진입부터 실제로 쓸 수 있게 될 때까지를 단계별로 잰다. **활성 세션은 하나뿐**이고, 결과는
 * 화면의 수명이 끝날 때([start]·[complete]·[abandonActive]) 확정된다 — 타이머를 쓰지 않는다.
 *
 * [Clock.monotonicNanos] 만 쓴다. 벽시계로 재면 NTP 동기화 한 번에 소요 시간이 음수가 되고 그게
 * 이탈로 오분류된다. 단일 세션·타이머 없음의 설계 근거는 #159.
 */
class TtiTracker(
    private val clock: Clock,
    private val observability: Observability,
    private val resourceSampler: ResourceSampler = ResourceSampler.NONE,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val slowThresholdMillis: Long = DEFAULT_SLOW_THRESHOLD_MILLIS,
) {
    private class Session(
        val page: TtiPage,
        val screen: ScreenKey,
        val startedNanos: Long,
    ) {
        val begun = mutableMapOf<TtiTimeline, Long>()
        val durations = mutableMapOf<TtiTimeline, Long>()
    }

    private var active: Session? = null

    /** 화면 진입. 진행 중인 세션이 있으면 이탈로 확정하고 정리한다. */
    @Synchronized
    fun start(
        page: TtiPage,
        screen: ScreenKey,
    ) {
        active?.let { emit(it) }
        active = Session(page, screen, clock.monotonicNanos())
    }

    /** 단계 시작. 이미 시작된 단계는 무시한다(첫 시각을 유지). */
    @Synchronized
    fun beginPhase(
        page: TtiPage,
        timeline: TtiTimeline,
    ) {
        val session = sessionFor(page) ?: return
        session.begun.putIfAbsent(timeline, clock.monotonicNanos())
    }

    /** 단계 종료. 시작 기록이 없으면 무시한다 — 짝이 안 맞는 구간을 0 으로 기록하지 않는다. */
    @Synchronized
    fun endPhase(
        page: TtiPage,
        timeline: TtiTimeline,
    ) {
        val session = sessionFor(page) ?: return
        val begunAt = session.begun[timeline] ?: return
        session.durations.putIfAbsent(timeline, (clock.monotonicNanos() - begunAt) / NANOS_PER_MILLI)
    }

    /** 화면이 쓸 수 있는 상태가 됐다. 결과를 확정해 내보낸다. */
    @Synchronized
    fun complete(page: TtiPage) {
        val session = sessionFor(page) ?: return
        active = null
        emit(session)
    }

    /**
     * 진행 중인 세션을 이탈로 확정한다. 다음 화면이 [start] 를 부르면 어차피 정리되지만,
     * **TTI 를 재지 않는 화면으로 이동하는 경우**가 있어 별도 진입점이 필요하다.
     */
    @Synchronized
    fun abandonActive() {
        val session = active ?: return
        active = null
        emit(session)
    }

    /** 세션이 이 페이지 것일 때만 반환한다. 뒤늦게 도착한 콜백이 새 세션을 오염시키지 않게 한다. */
    private fun sessionFor(page: TtiPage): Session? = active?.takeIf { it.page === page }

    private fun emit(session: Session) {
        if (active === session) active = null
        val totalMillis = (clock.monotonicNanos() - session.startedNanos) / NANOS_PER_MILLI
        val phases = session.page.timelines.mapNotNull { t -> session.durations[t]?.let { TtiPhase(t, it) } }
        val outcome =
            when {
                totalMillis >= timeoutMillis -> TtiOutcome.TIMEOUT
                phases.size < session.page.timelines.size -> TtiOutcome.ABANDONED
                else -> TtiOutcome.COMPLETED
            }
        observability.log(Channel.PERFORMANCE) {
            PerformancePayload.Tti(
                screen = session.screen,
                phases = phases,
                pageName = session.page.pageName,
                totalMillis = totalMillis,
                outcome = outcome,
            )
        }
        if (totalMillis >= slowThresholdMillis) emitResourceProbe()
    }

    private fun emitResourceProbe() {
        val probe = resourceSampler.sample(ProbeTrigger.TTI_SLOW) ?: return
        observability.log(Channel.PERFORMANCE) { probe }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val DEFAULT_TIMEOUT_MILLIS = 20_000L

        /** 이보다 오래 걸린 화면은 자원 스냅샷을 함께 남긴다. */
        const val DEFAULT_SLOW_THRESHOLD_MILLIS = 2_000L
    }
}
