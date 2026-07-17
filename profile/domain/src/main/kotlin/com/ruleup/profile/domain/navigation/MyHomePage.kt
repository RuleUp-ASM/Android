package com.ruleup.profile.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 마이 홈 페이지. 하단 MY 탭으로 진입하는 마이 탭 루트 화면. */
data object MyHomePage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_HOME
}
