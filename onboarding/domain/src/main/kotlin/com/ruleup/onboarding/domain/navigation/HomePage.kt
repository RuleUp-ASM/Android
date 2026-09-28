package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 홈. */
object HomePage : Page {
    const val PATH = AppRoutes.HOME

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
