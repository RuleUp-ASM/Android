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
 * 관측 파이프라인 배선.
 *
 * **[BuildProfile] 은 `:app` 이 제공해야 한다.** 이 모듈은 앱의 `BuildConfig` 를 볼 수 없고,
 * 라이브러리 자체 `BuildConfig.DEBUG` 는 빌드 타입만 알아서 QA 플레이버를 구분하지 못한다.
 * 바인딩이 없으면 `:app` 컴파일 시점에 Hilt 가 실패한다.
 *
 * 출구는 **데코레이터 체인**으로 조립한다. 채널 라우팅·심각도 임계값이 각각 독립한 데코레이터라
 * *"어떤 이벤트가 어디로 가는지"* 가 이 파일 한 곳에 드러난다 — 어댑터 안에 숨으면 그게 곧
 * [Policy] 설정에도 안 잡히는 "안 찍히는 이유"가 된다.
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
     * 출구 체인.
     *
     * ```
     * CompositeSink
     *   ├ ChannelFilterSink(BUSINESS, PERFORMANCE) → FirebaseAnalyticsSink
     *   ├ ChannelFilterSink(BUSINESS, PERFORMANCE) → AmplitudeSink        (키가 있을 때만)
     *   ├ ChannelFilterSink(DIAGNOSTIC) → SeverityFilterSink(WARN) → CrashlyticsSink
     *   ├ LogcatSink                                        (프로덕션 제외)
     *   └ [extraSinks]                                      (debug 변형의 인스펙터 등)
     * ```
     *
     * 성능 채널을 Analytics 로 보낸다 — Firebase Performance SDK 는 커스텀 지표가 제한적이라
     * 우선 이벤트로 수집한다. 자체 서버가 생기면 이 줄만 바꾸면 된다.
     *
     * 진단 채널의 `WARN` 하한은 **Crashlytics 쿼터 보호**용이다. 그보다 낮은 진단은
     * 개발 중 [LogcatSink] 와 인스펙터로 본다.
     *
     * Amplitude 는 Firebase 와 **병행**한다 — 같은 이벤트가 두 곳에 쌓이므로 집계할 때 출처를 섞지
     * 않는다. Amplitude 로 확정되면 Firebase 줄을 빼면 된다.
     *
     * 키가 비어 있으면 **출구를 아예 달지 않는다.** 빈 키로 SDK 를 띄우면 전송이 조용히 실패해
     * "왜 안 올라가지"를 한참 뒤에 알게 된다.
     *
     * [extraSinks] 는 다른 모듈이 `@IntoSet Sink` 로 기여한 출구다. 릴리스에서는 비어 있다.
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

    /**
     * 화면 TTI 추적기. 활성 세션이 하나뿐이라 앱 전역 싱글턴이다.
     *
     * 각 화면은 자기 [com.ruleup.observability.domain.model.TtiPage] 를 선언하고 단계를 표시한다.
     */
    @Provides
    @Singleton
    fun ttiTracker(
        clock: Clock,
        observability: Observability,
        resourceSampler: ResourceSampler,
    ): TtiTracker = TtiTracker(clock, observability, resourceSampler)
}
