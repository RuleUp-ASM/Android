package com.ruleup.domain.test

/** Robolectric 에서 전역 클릭 가드를 넘기기 위한 단조 증가 시계 오프셋. */
object ClickClock {
    private const val STEP_MILLIS = 1_500L

    private var elapsed = 0L

    /** 호출할 때마다 이전보다 큰 오프셋. */
    fun nextOffsetMillis(): Long {
        elapsed += STEP_MILLIS
        return elapsed
    }
}
