package com.ruleup.android_ruleup.di

import android.content.Context
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * 앱 전역 DI 그래프. 각 모듈이 @ContributesBinding/@ContributesTo/@ContributesIntoMap 로
 * AppScope 에 기여한 바인딩을 한곳에 모은다.
 *
 * [ViewModelGraph] 를 상속해 MetroX 의 ViewModel 멀티바인딩과 [dev.zacsweers.metrox.viewmodel.MetroViewModelFactory]
 * 접근자를 함께 제공한다.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    val navigationHelper: NavigationHelper
    val messageHelper: MessageHelper

    @DependencyGraph.Factory
    fun interface Factory {
        // Application 컨텍스트를 그래프 바인딩으로 주입한다(datastore/messageHelper/profile 업로드 등에서 사용).
        fun create(
            @Provides context: Context,
        ): AppGraph
    }
}
