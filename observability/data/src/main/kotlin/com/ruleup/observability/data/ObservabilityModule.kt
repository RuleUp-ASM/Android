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

/** 관측 파이프라인 배선. */
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

    /** 출구 체인. */
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
                        channels = setOf(Channel.PERFORMANCE),
                        delegate = FirebaseAnalyticsSink(context),
                    ),
                )
                if (amplitudeApiKey.isConfigured) {
                    add(
                        ChannelFilterSink(
                            channels = setOf(Channel.PERFORMANCE),
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
}
