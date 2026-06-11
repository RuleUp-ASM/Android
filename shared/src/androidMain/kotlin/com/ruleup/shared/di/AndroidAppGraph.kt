package com.ruleup.shared.di

import android.content.Context
import com.ruleup.network.di.BaseUrl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/**
 * Android 앱 전역 DI 그래프. 각 모듈이 @ContributesBinding/@ContributesTo/@ContributesIntoMap 로
 * AppScope 에 기여한 바인딩을 한곳에 모은다. Application Context 를 그래프에 주입한다
 * (datastore/messageHelper/이미지 업로드 등에서 사용).
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
            @Provides @BaseUrl baseUrl: String,
        ): AndroidAppGraph
    }
}
