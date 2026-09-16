package com.ruleup.logging.domain.test

import com.ruleup.logging.domain.BizLogClock

/** 손으로 감는 시계. 흐르게 하지 않으면 멈춰 있다. */
class FakeBizLogClock(
    var nowMillis: Long = 0L,
) : BizLogClock {
    fun advance(millis: Long) {
        nowMillis += millis
    }

    override fun nowMillis(): Long = nowMillis
}
