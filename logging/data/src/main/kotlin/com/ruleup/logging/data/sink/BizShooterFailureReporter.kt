package com.ruleup.logging.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** 전송 실패 집계. */
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

        /** `(전송기#예외타입) → 누적 실패 수`. */
        fun snapshot(): Map<String, Long> = counters.mapValues { it.value.get() }
    }
