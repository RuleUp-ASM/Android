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
 * 화면 진입부터 실제로 쓸 수 있게 될 때까지를 단계별로 잰다.
 *
 * ## 세션이 하나뿐이다
 * 화면은 순차적으로 열리므로 **활성 세션을 하나만 들고**, 새 세션이 시작되면 이전 것을 정리한다.
 * 페이지별 맵을 두면 같은 화면에 재진입할 때 이전 추적이 조용히 덮이면서, 이미 버려진 세션이
 * 뒤늦게 결과를 쏘는 유령 리포트가 생긴다.
 *
 * ## 타이머를 쓰지 않는다
 * 제한 시간이 지나면 강제 종료하는 흔한 방식 대신, **화면이 바뀌는 순간**([start]·[abandonActive])
 * 정리한다. 네비게이션이 이미 자연스러운 경계라 별도 타이머가 필요 없고, 도메인이 코루틴에
 * 의존하지 않아도 된다. 사용자가 느린 화면에 계속 머무는 동안 섣불리 "타임아웃"을 쏘지 않는다는
 * 점에서 더 정확하기도 하다 — 결과는 화면의 수명이 끝날 때 확정된다.
 *
 * ## 느린 화면에는 자원 스냅샷이 따라붙는다
 * 총 시간이 [slowThresholdMillis] 를 넘으면 [ResourceSampler] 로 그 순간의 메모리·CPU 를 함께
 * 남긴다. "3초 걸렸다" 만으로는 네트워크가 느린 건지 기기가 쪼들린 건지 갈리지 않는다.
 * 실제 방출 여부는 샘플러의 쓰로틀이 정한다.
 *
 * ## 시간
 * [Clock.monotonicNanos] 만 쓴다. 벽시계로 재면 NTP 동기화 한 번에 소요 시간이 음수가 되고,
 * 그게 이탈로 오분류된다.
 *
 * 모든 메서드가 임의 스레드에서 호출될 수 있어 상태 전이를 통째로 잠근다. 맵 갱신 몇 줄이라
 * 디스패처로 넘기는 것보다 싸다.
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
     * 진행 중인 세션을 이탈로 확정한다.
     *
     * 네비게이션이 일어날 때 호출한다. 다음 화면이 [start] 를 부르면 어차피 정리되지만,
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
