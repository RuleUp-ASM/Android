package com.ruleup.report.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 신고한 사용자·챌린지 페이지 (Figma `1286:30`).
 *
 * Figma 는 설정 허브에서 들어가지만 앱에는 아직 설정 화면이 없어 마이 홈 메뉴에 단다.
 * 설정 화면이 생기면 진입점만 옮기면 되고 이 경로는 그대로 쓴다.
 */
data object BlockListPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_BLOCKS
}
