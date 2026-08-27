package com.ruleup.observability.data

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.observability.domain.model.ProbeTrigger
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ResourceSampler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기 자원 스냅샷. [ActivityManager.getMemoryInfo] 가 바인더 호출이라 **트리거별 최소 간격**을 두고
 * 걸리면 null 을 준다 — jank 창마다(스크롤 중이면 5초마다) 부르면 jank 를 재려다 jank 를 만든다.
 *
 * [ProbeTrigger.PERIODIC] 표본은 기준선용이다. 없으면 "jank 때 힙 120MB" 를 해석할 수 없다.
 */
@Singleton
class ResourceProbeCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
    ) : ResourceSampler {
        private val activityManager by lazy { context.getSystemService(ActivityManager::class.java) }
        private val lastProbeNanos = mutableMapOf<ProbeTrigger, Long>()

        private var lastCpuMillis = 0L
        private var lastCpuWallNanos = 0L

        /** 쓰로틀에 걸리면 null. 호출부는 그냥 건너뛰면 된다. */
        @Synchronized
        override fun sample(trigger: ProbeTrigger): PerformancePayload.ResourceProbe? {
            val now = clock.monotonicNanos()
            val minInterval = minIntervalNanos(trigger)
            val last = lastProbeNanos[trigger]
            if (last != null && now - last < minInterval) return null
            lastProbeNanos[trigger] = now

            val runtime = Runtime.getRuntime()
            val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
            return PerformancePayload.ResourceProbe(
                trigger = trigger,
                heapUsedBytes = runtime.totalMemory() - runtime.freeMemory(),
                heapMaxBytes = runtime.maxMemory(),
                nativeHeapBytes = Debug.getNativeHeapAllocatedSize(),
                availableBytes = memoryInfo.availMem,
                lowMemory = memoryInfo.lowMemory,
                cpuPercent = cpuPercentSinceLastProbe(now),
            )
        }

        /**
         * 직전 표본 이후 이 프로세스가 쓴 CPU 비율. `/proc` 대신 [Process.getElapsedCpuTime] 델타만 써 IO 가 없다.
         * **멀티코어에서 100을 넘을 수 있다**(4코어를 다 쓰면 400). 첫 호출은 기준점이 없어 null.
         */
        private fun cpuPercentSinceLastProbe(nowNanos: Long): Double? {
            val cpuMillis = Process.getElapsedCpuTime()
            val previousCpu = lastCpuMillis
            val previousWall = lastCpuWallNanos
            lastCpuMillis = cpuMillis
            lastCpuWallNanos = nowNanos
            if (previousWall == 0L) return null
            val wallMillis = (nowNanos - previousWall) / NANOS_PER_MILLI
            if (wallMillis <= 0) return null
            return (cpuMillis - previousCpu) * PERCENT / wallMillis.toDouble()
        }

        private fun minIntervalNanos(trigger: ProbeTrigger): Long =
            when (trigger) {
                // 느린 순간은 자주 오므로 짧게. 그래도 창(5초)마다는 아니다.
                ProbeTrigger.JANK_DETECTED, ProbeTrigger.TTI_SLOW -> ANOMALY_INTERVAL_MILLIS
                ProbeTrigger.PERIODIC -> BASELINE_INTERVAL_MILLIS
            } * NANOS_PER_MILLI

        private companion object {
            const val NANOS_PER_MILLI = 1_000_000L
            const val PERCENT = 100
            const val ANOMALY_INTERVAL_MILLIS = 30_000L
            const val BASELINE_INTERVAL_MILLIS = 300_000L
        }
    }
