package com.ruleup.domain.helper

import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow

/**
 * 단일 네비게이션 플로우. 전진/후진 모두 [NavSignal] 한 가지 형식으로 emit 된다.
 *
 * 호출부는 다음 중 하나로 사용한다.
 * - [navigateByRoute] — 직접 [NavRoute] 를 구성해서 전진 이동.
 * - [navigateTo] — 각 feature 가 정의한 [Page] 객체를 그대로 전달 (권장).
 * - [navigateToBack] — 하드웨어 백 키와 동일하게 한 단계 뒤로 이동.
 */
interface NavigationHelper {
    val navigationFlow: Flow<NavSignal>

    fun navigateByRoute(route: NavRoute)

    fun navigateTo(page: Page)

    /**
     * 백스택을 [route] 의 시작 스택으로 교체한다. 스플래시가 인증 후 딥링크 목적지로 보낼 때 쓴다.
     * 일반 화면 이동에는 쓰지 않는다 — 되돌아갈 곳이 사라진다.
     */
    fun replaceStackWith(route: NavRoute)

    fun navigateToBack()
}
