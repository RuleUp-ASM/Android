package com.ruleup.observability.data

import android.content.Context
import com.ruleup.observability.data.clock.RealClock
import com.ruleup.observability.data.context.ScreenContextHolder
import com.ruleup.observability.data.policy.RuntimePolicy
import com.ruleup.observability.data.policy.defaultPolicyConfig
import com.ruleup.observability.data.sink.AmplitudeSink
import com.ruleup.observability.data.sink.ChannelFilterSink
import com.ruleup.observability.data.sink.CompositeSink
import com.ruleup.observability.data.sink.CrashlyticsSink
import com.ruleup.observability.data.sink.FirebaseAnalyticsSink
import com.ruleup.observability.data.sink.LogcatSink
import com.ruleup.observability.data.sink.SeverityFilterSink
import com.ruleup.observability.data.sink.SinkFailureReporter
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.TtiTracker
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.AmplitudeApiKey
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.port.Clock
import com.ruleup.observability.domain.port.ContextProvider
import com.ruleup.observability.domain.port.Policy
import com.ruleup.observability.domain.port.ResourceSampler
import com.ruleup.observability.domain.port.Sink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 관측 파이프라인 배선. **[BuildProfile] 은 `:app` 이 제공해야 한다** — 이 모듈은 앱의 `BuildConfig` 를
 * 볼 수 없고, 라이브러리 자체 `BuildConfig.DEBUG` 는 QA 플레이버를 구분하지 못한다.
 *
 * 출구는 데코레이터 체인으로 조립한다. 라우팅·임계값을 어댑터 안에 숨기면 그게 곧 [Policy] 설정에도
 * 안 잡히는 "안 찍히는 이유"가 된다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ObservabilityModule {
    @Provides
    @Singleton
    fun clock(): Clock = RealClock

    @Provides
    fun contextProvider(holder: ScreenContextHolder): ContextProvider = holder

    @Provides
    @Singleton
    fun runtimePolicy(profile: BuildProfile): RuntimePolicy = RuntimePolicy(defaultPolicyConfig(profile))

    @Provides
    fun policy(policy: RuntimePolicy): Policy = policy

    @Provides
    fun resourceSampler(collector: ResourceProbeCollector): ResourceSampler = collector

    /**
     * 출구 체인. 진단의 `WARN` 하한은 **Crashlytics 쿼터 보호**용이고, 성능 채널이 Analytics 로 가는
     * 건 Firebase Performance 의 커스텀 지표가 제한적이어서다(자체 서버가 생기면 이 줄만 바꾼다).
     *
     * Amplitude 는 Firebase 와 **병행**한다 — 같은 이벤트가 두 곳에 쌓이므로 집계할 때 출처를 섞지
     * 않는다. 키가 비면 출구를 아예 달지 않는 이유는 #259.
     */
    @Provides
    @Singleton
    fun sink(
        @ApplicationContext context: Context,
        profile: BuildProfile,
        amplitudeApiKey: AmplitudeApiKey,
        failureReporter: SinkFailureReporter,
        extraSinks: Set<@JvmSuppressWildcards Sink>,
    ): Sink {
        val children =
            buildList {
                add(
                    ChannelFilterSink(
                        channels = setOf(Channel.BUSINESS, Channel.PERFORMANCE),
                        delegate = FirebaseAnalyticsSink(context),
                    ),
                )
                if (amplitudeApiKey.isConfigured) {
                    add(
                        ChannelFilterSink(
                            channels = setOf(Channel.BUSINESS, Channel.PERFORMANCE),
                            delegate = AmplitudeSink(context, amplitudeApiKey, profile),
                        ),
                    )
                }
                add(
                    ChannelFilterSink(
                        channels = setOf(Channel.DIAGNOSTIC),
                        delegate = SeverityFilterSink(min = Severity.WARN, delegate = CrashlyticsSink()),
                    ),
                )
                if (profile.isDebuggable) add(LogcatSink())
                addAll(extraSinks)
            }
        return CompositeSink(children, profile, failureReporter)
    }

    @Provides
    @Singleton
    fun observability(
        clock: Clock,
        contextProvider: ContextProvider,
        profile: BuildProfile,
        policy: Policy,
        sink: Sink,
    ): Observability =
        Observability(
            clock = clock,
            contextProvider = contextProvider,
            profile = profile,
            policy = policy,
            sink = sink,
        )

    /** 화면 TTI 추적기. 활성 세션이 하나뿐이라 앱 전역 싱글턴이다. */
    @Provides
    @Singleton
    fun ttiTracker(
        clock: Clock,
        observability: Observability,
        resourceSampler: ResourceSampler,
    ): TtiTracker = TtiTracker(clock, observability, resourceSampler)
}
