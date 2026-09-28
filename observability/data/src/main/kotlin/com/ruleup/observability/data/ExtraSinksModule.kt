package com.ruleup.observability.data

import com.ruleup.observability.domain.port.Sink
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** 다른 모듈이 `@IntoSet Sink` 로 출구를 더할 수 있게 여는 주입구. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExtraSinksModule {
    @Multibinds
    abstract fun extraSinks(): Set<Sink>
}
