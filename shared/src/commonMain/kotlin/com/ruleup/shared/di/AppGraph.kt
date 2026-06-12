package com.ruleup.shared.di

import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * 플랫폼 공통 앱 그래프 계약. 실제 [dev.zacsweers.metro.DependencyGraph] 는 플랫폼별로 정의한다
 * (androidMain 의 AndroidAppGraph 는 Context 를, iosMain 의 IosAppGraph 는 baseUrl 만 받는다).
 *
 * [ViewModelGraph] 를 상속해 MetroX 의 ViewModel 멀티바인딩과 MetroViewModelFactory 접근자를 함께 제공한다.
 */
interface AppGraph : ViewModelGraph {
    val navigationHelper: NavigationHelper
    val messageHelper: MessageHelper
}
