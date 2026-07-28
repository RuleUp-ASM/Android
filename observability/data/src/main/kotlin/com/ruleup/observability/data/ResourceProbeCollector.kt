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
 * 기기 자원 스냅샷을 뜬다. **느렸던 순간의 상태를 붙여 두기 위한 것**이다.
 *
 * `JankWindow` 만으로는 "느렸다"까지밖에 모른다. 그때 힙이 한계에 붙어 있었는지, 시스템이
 * 저메모리였는지가 있어야 원인 방향이 잡힌다. [ProbeTrigger] 가 그 연결을 명시한다.
 *
 * ## 쓰로틀이 필수다
 * [ActivityManager.getMemoryInfo] 는 바인더 호출이다. jank 창이 닫힐 때마다(스크롤 중이면 5초마다)
 * 부르면 **관측이 관측 대상을 바꾼다** — jank 를 재려다 jank 를 만든다. 그래서 트리거별로
 * 최소 간격을 두고, 걸리면 null 을 돌려준다.
 *
 * 기준선이 없으면 "jank 때 힙 120MB" 는 해석할 수 없다. 그래서 정상 구간의
 * [ProbeTrigger.PERIODIC] 표본을 **더 긴 간격으로** 함께 뜬다.
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
         * 직전 표본 이후 이 프로세스가 쓴 CPU 비율.
         *
         * `/proc` 을 뒤지지 않고 [Process.getElapsedCpuTime] 델타만 쓴다 — 바인더도 파일 IO 도 없다.
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
                // 기준선은 드물어도 된다.
                ProbeTrigger.PERIODIC -> BASELINE_INTERVAL_MILLIS
            } * NANOS_PER_MILLI

        private companion object {
            const val NANOS_PER_MILLI = 1_000_000L
            const val PERCENT = 100
            const val ANOMALY_INTERVAL_MILLIS = 30_000L
            const val BASELINE_INTERVAL_MILLIS = 300_000L
        }
    }
