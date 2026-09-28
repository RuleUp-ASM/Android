package com.ruleup.observability.data.sink

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** 싱크 실패 집계. */
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

        /** 인스펙터·메타 지표용 스냅샷. */
        fun snapshot(): Map<String, Long> = counters.mapValues { it.value.get() }
    }
