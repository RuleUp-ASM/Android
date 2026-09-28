package com.ruleup.observability.domain.test

import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ContextProvider
import com.ruleup.observability.domain.port.Policy
import com.ruleup.observability.domain.port.Sink

/** 테스트용 [Observability] 조립기. */
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
