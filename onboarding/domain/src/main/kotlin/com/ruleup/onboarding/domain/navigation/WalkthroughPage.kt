package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 첫 실행 워크쓰루. 스플래시가 미로그인·미열람일 때만 열고, 끝나면 로그인으로 넘긴다. */
object WalkthroughPage : Page {
    const val PATH = AppRoutes.WALKTHROUGH

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
