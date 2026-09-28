package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 스플래시. */
object SplashPage : Page {
    const val PATH = AppRoutes.SPLASH

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
