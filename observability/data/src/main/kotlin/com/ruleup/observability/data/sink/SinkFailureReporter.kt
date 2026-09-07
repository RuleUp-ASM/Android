package com.ruleup.observability.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 싱크 실패 집계. 남지 않으면 *"수집이 조용히 멈춘 상태"* 와 *"이벤트가 원래 없는 상태"* 가 구분되지
 * 않아 `google-services.json` 누락 같은 사고를 배포 몇 주 뒤에야 알게 된다.
 *
 * **첫 발생만** `recordException` 으로 남기고 이후는 센다 — 초당 수천 번 실패해도 Crashlytics 로 가는
 * 건 1건이다. Crashlytics 를 직접 부르는 건 고장난 파이프라인으로 신호를 보낼 수 없어서다(무한 재귀).
 */
@Singleton
class SinkFailureReporter
    @Inject
    constructor() {
        private val counters = ConcurrentHashMap<String, AtomicLong>()
        private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

        fun onFailure(
            sinkName: String,
            cause: Throwable,
        ) {
            val key = "$sinkName#${cause.javaClass.name}"
            val counter = counters.computeIfAbsent(key) { AtomicLong() }
            val occurrence = counter.incrementAndGet()
            if (occurrence == 1L) {
                crashlytics.setCustomKey("obs_failed_sink", sinkName)
                crashlytics.recordException(cause)
            }
        }

        /** 인스펙터·메타 지표용 스냅샷. `(싱크#예외타입) → 누적 실패 수`. */
        fun snapshot(): Map<String, Long> = counters.mapValues { it.value.get() }
    }
