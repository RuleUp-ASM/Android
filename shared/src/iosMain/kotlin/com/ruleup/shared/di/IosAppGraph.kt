package com.ruleup.shared.di

import com.ruleup.network.di.BaseUrl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/**
 * iOS 앱 전역 DI 그래프. Android 와 달리 Context 가 없어 baseUrl 만 받는다.
 * datastore(NSDocumentDirectory)·messageHelper 등 Context 비의존 iosMain 바인딩을 집계한다.
 */
@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides @BaseUrl baseUrl: String,
        ): IosAppGraph
    }
}
