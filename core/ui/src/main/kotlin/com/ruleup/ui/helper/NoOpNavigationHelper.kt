package com.ruleup.ui.helper

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 아무것도 하지 않는 [NavigationHelper]. **`@Preview` 전용이다.**
 *
 * 프리뷰는 [LocalNavigationHelper] 를 읽는 Content 를 그리기 위해 값을 채워야 하는데, 실제 구현체는
 * app 이 갖고 있어(feature 에서 참조 불가) 여기 더미를 둔다. 신호를 흘려보내지 않으므로 프리뷰에서
 * 버튼을 눌러도 아무 일도 일어나지 않는다 — 의도된 동작이다.
 */
object NoOpNavigationHelper : NavigationHelper {
    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateByRoute(route: NavRoute) = Unit

    override fun navigateTo(page: Page) = Unit

    override fun replaceStackWith(route: NavRoute) = Unit

    override fun navigateToBack() = Unit
}
