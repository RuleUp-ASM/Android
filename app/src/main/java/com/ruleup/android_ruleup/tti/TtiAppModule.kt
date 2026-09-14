package com.ruleup.android_ruleup.tti

import com.ruleup.tti.domain.TtiShooter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** TTI 가 어디로 나갈지 정하는 곳. `:tti:data` 가 이 바인딩을 일부러 비워 뒀다. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TtiAppModule {
    @Binds
    @Singleton
    abstract fun bindTtiShooter(impl: ObservabilityTtiShooter): TtiShooter
}
