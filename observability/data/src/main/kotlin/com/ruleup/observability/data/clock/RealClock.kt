package com.ruleup.observability.data.clock

import com.ruleup.observability.domain.port.Clock

internal object RealClock : Clock {
    override fun epochMillis(): Long = System.currentTimeMillis()

    override fun monotonicNanos(): Long = System.nanoTime()
}
