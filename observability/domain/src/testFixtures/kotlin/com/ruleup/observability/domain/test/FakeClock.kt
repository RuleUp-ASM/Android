package com.ruleup.observability.domain.test

import com.ruleup.observability.domain.port.Clock

/** 수동으로 전진시키는 시계. */
class FakeClock(
    var nowMillis: Long = 0L,
    var nanos: Long = 0L,
) : Clock {
    override fun epochMillis(): Long = nowMillis

    override fun monotonicNanos(): Long = nanos

    fun advanceMillis(delta: Long) {
        nowMillis += delta
        nanos += delta * 1_000_000
    }
}
