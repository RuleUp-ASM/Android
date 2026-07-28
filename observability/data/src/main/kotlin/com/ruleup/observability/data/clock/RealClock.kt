package com.ruleup.observability.data.clock

import com.ruleup.observability.domain.port.Clock

/**
 * 시스템 시계
 */
internal object RealClock : Clock {
    override fun epochMillis(): Long = System.currentTimeMillis()

    override fun monotonicNanos(): Long = System.nanoTime()
}
