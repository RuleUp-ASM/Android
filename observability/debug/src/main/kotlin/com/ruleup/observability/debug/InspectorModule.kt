package com.ruleup.observability.debug

import com.ruleup.observability.domain.port.Sink
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * 인스펙터 싱크를 파이프라인의 **추가 출구**로 등록한다. `ObservabilityModule` 이 `Set<Sink>` 로
 * 받아 합성에 끼워 넣고, 이 모듈은 debug 변형에만 있어 릴리스에서는 그 집합이 비어 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InspectorModule {
    @Binds
    @IntoSet
    @Singleton
    abstract fun inspectorSink(impl: InspectorSink): Sink
}
