package com.ruleup.observability.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 싱크 실패 집계.
 *
 * 프로덕션에서 파이프라인 실패를 삼키기로 한 이상, **그 사실은 반드시 어딘가 남아야 한다.**
 * 남지 않으면 *"수집이 조용히 멈춘 상태"* 와 *"이벤트가 원래 없는 상태"* 가 구분되지 않는다 —
 * `google-services.json` 누락 같은 사고가 배포 몇 주 뒤에야 발견된다.
 *
 * **첫 발생은 풍부하게, 이후는 카운터.** `(싱크, 예외 타입)` 조합마다 처음 한 번만
 * `recordException` 으로 스택·기기·OS 를 남기고, 그 뒤로는 수만 센다. 초당 수천 번 실패해도
 * Crashlytics 로 가는 건 1건이고 원인 파악에 필요한 정보는 확보된다.
 *
 * **파이프라인을 거치지 않는다.** Crashlytics 를 직접 부른다 — 고장난 경로로 신호를 보낼 수 없고,
 * 관측자가 Observability 를 다시 부르면 무한 재귀가 된다.
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
