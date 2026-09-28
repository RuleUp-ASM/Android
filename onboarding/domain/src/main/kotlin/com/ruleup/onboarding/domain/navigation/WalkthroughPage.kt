package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 첫 실행 워크쓰루. */
object WalkthroughPage : Page {
    const val PATH = AppRoutes.WALKTHROUGH

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
