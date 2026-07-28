package com.ruleup.observability.domain.test

import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ContextProvider
import com.ruleup.observability.domain.port.Policy
import com.ruleup.observability.domain.port.Sink

/**
 * 테스트용 [Observability] 조립기.
 *
 * [Observability] 는 인터페이스가 아니라 구체 클래스라 목을 만들 수 없다. 대신 포트를 전부
 * 테스트 대역으로 채운 **실제 인스턴스**를 만든다 — 게이트·정화·디스패치가 실제로 도는 상태에서
 * 검증하게 되므로 목보다 강한 테스트가 된다.
 *
 * [Policy]·[ContextProvider] 가 모두 `fun interface` 라 람다로 끝난다.
 *
 * 기본값은 **아무것도 막지 않는** 파이프라인이다. 게이트를 검증할 때만 [policy] 를 바꾼다.
 */
fun testObservability(
    sink: Sink = RecordingSink(),
    policy: Policy = Policy { _, _, _ -> true },
    clock: Clock = FakeClock(),
    screen: ScreenKey? = null,
    profile: BuildProfile = BuildProfile.DEV,
): Observability =
    Observability(
        clock = clock,
        contextProvider = ContextProvider { ObsContext(currentScreen = screen) },
        profile = profile,
        policy = policy,
        sink = sink,
    )
