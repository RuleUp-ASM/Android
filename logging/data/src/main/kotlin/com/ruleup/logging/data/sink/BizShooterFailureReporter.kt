package com.ruleup.logging.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 전송 실패 집계. 남지 않으면 *"수집이 조용히 멈춘 상태"* 와 *"행동이 원래 없는 상태"* 가 구분되지
 * 않아, 전환율이 0 으로 보이는 원인을 배포 몇 주 뒤에야 찾게 된다.
 *
 * **첫 발생만** `recordException` 으로 남기고 이후는 센다 — 초당 수천 번 실패해도 Crashlytics 로 가는
 * 건 1건이다.
 */
@Singleton
class BizShooterFailureReporter
    @Inject
    constructor() {
        private val counters = ConcurrentHashMap<String, AtomicLong>()
        private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

        fun onFailure(
            shooterName: String,
            cause: Throwable,
        ) {
            val key = "$shooterName#${cause.javaClass.name}"
            val occurrence = counters.computeIfAbsent(key) { AtomicLong() }.incrementAndGet()
            if (occurrence == 1L) {
                crashlytics.setCustomKey("bizlog_failed_shooter", shooterName)
                crashlytics.recordException(cause)
            }
        }

        /** `(전송기#예외타입) → 누적 실패 수`. 비어 있지 않으면 수집이 조용히 멈춘 구간이 있다. */
        fun snapshot(): Map<String, Long> = counters.mapValues { it.value.get() }
    }
