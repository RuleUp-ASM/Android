package com.ruleup.observability.domain.test

import com.ruleup.observability.domain.port.Clock

/**
 * 수동으로 전진시키는 시계.
 *
 * 시간에 의존하는 정책(중복 제거 창·레이트리밋)을 `Thread.sleep` 없이 테스트하기 위한 것이다.
 * 두 시계를 함께 전진시켜 벽시계와 단조 시계가 어긋나지 않게 한다 — 어긋난 상황을 재현하려면
 * [nowMillis] 만 직접 바꾸면 된다(NTP 역행 시나리오).
 */
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
