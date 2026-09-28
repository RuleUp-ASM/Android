package com.ruleup.observability.debug

import com.ruleup.observability.domain.port.Sink
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/** 인스펙터 싱크를 파이프라인의 추가 출구로 등록한다. */
@Module
@InstallIn(SingletonComponent::class)
abstract class InspectorModule {
    @Binds
    @IntoSet
    @Singleton
    abstract fun inspectorSink(impl: InspectorSink): Sink
}
