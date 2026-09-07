package com.ruleup.domain.test

/**
 * Robolectric 에서 전역 클릭 가드를 넘기기 위한 **단조 증가** 시계 오프셋.
 *
 * `core:designsystem` 의 `SingleClickGuard` 는 마지막 클릭 시각을 `object` 필드로 들고 있어
 * 테스트를 건너 살아남는데, Robolectric 은 `SystemClock` 을 테스트마다 되감는다. 그래서 매번
 * 같은 양만 밀면 두 번째 테스트부터 차이가 음수가 되어 **클릭이 조용히 삼켜진다** — 하나만
 * 돌리면 통과하는데 클래스 전체를 돌리면 깨지는, 원인을 찾기 가장 어려운 형태다.
 *
 * 누적해서 밀면 되감긴 시계에서도 직전 테스트가 남긴 값보다 항상 앞선다.
 * 실제 전진은 Robolectric 을 아는 쪽(presentation 테스트)이 [nextOffsetMillis] 로 받아서 한다 —
 * `core:domain` 은 순수 JVM 이라 Robolectric 을 의존할 수 없다.
 */
object ClickClock {
    private const val STEP_MILLIS = 1_500L

    private var elapsed = 0L

    /** 호출할 때마다 이전보다 큰 오프셋. 클릭 직전에 이만큼 시계를 민다. */
    fun nextOffsetMillis(): Long {
        elapsed += STEP_MILLIS
        return elapsed
    }
}
